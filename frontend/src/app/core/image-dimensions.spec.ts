import { readImageDimensions } from './image-dimensions';

describe('readImageDimensions', () => {
  it('reads PNG dimensions without decoding the image', async () => {
    const bytes = new Uint8Array(24);
    bytes.set([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
    bytes.set([0x49, 0x48, 0x44, 0x52], 12);
    writeUint32BigEndian(bytes, 16, 4032);
    writeUint32BigEndian(bytes, 20, 3024);

    await expect(readImageDimensions(new Blob([bytes], { type: 'image/png' })))
      .resolves.toEqual({ width: 4032, height: 3024 });
  });

  it('reads JPEG dimensions from a start-of-frame segment', async () => {
    const bytes = new Uint8Array(21);
    bytes.set([0xff, 0xd8, 0xff, 0xc0, 0x00, 0x11, 0x08, 0x0b, 0xd0, 0x0f, 0xc0]);

    await expect(readImageDimensions(new Blob([bytes], { type: 'image/jpeg' })))
      .resolves.toEqual({ width: 4032, height: 3024 });
  });

  it('reads extended WebP canvas dimensions', async () => {
    const bytes = new Uint8Array(30);
    bytes.set([0x52, 0x49, 0x46, 0x46], 0);
    bytes.set([0x57, 0x45, 0x42, 0x50], 8);
    bytes.set([0x56, 0x50, 0x38, 0x58], 12);
    writeUint24LittleEndian(bytes, 24, 1599);
    writeUint24LittleEndian(bytes, 27, 899);

    await expect(readImageDimensions(new Blob([bytes], { type: 'image/webp' })))
      .resolves.toEqual({ width: 1600, height: 900 });
  });

  it('rejects content that does not match its declared type', async () => {
    await expect(readImageDimensions(new Blob([new Uint8Array(24)], { type: 'image/png' })))
      .rejects.toThrow('Invalid PNG image.');
  });
});

function writeUint32BigEndian(bytes: Uint8Array, offset: number, value: number): void {
  bytes[offset] = value >>> 24;
  bytes[offset + 1] = value >>> 16;
  bytes[offset + 2] = value >>> 8;
  bytes[offset + 3] = value;
}

function writeUint24LittleEndian(bytes: Uint8Array, offset: number, value: number): void {
  bytes[offset] = value;
  bytes[offset + 1] = value >>> 8;
  bytes[offset + 2] = value >>> 16;
}
