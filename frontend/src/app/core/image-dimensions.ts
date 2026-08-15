export interface ImageDimensions {
  width: number;
  height: number;
}

const jpegStartOfFrameMarkers = new Set([
  0xc0, 0xc1, 0xc2, 0xc3,
  0xc5, 0xc6, 0xc7,
  0xc9, 0xca, 0xcb,
  0xcd, 0xce, 0xcf
]);

export async function readImageDimensions(image: Blob): Promise<ImageDimensions> {
  const bytes = new Uint8Array(await image.arrayBuffer());
  const contentType = image.type.toLowerCase();

  if (contentType === 'image/jpeg') {
    return readJpegDimensions(bytes);
  }
  if (contentType === 'image/png') {
    return readPngDimensions(bytes);
  }
  if (contentType === 'image/webp') {
    return readWebpDimensions(bytes);
  }

  throw new Error('Unsupported image type.');
}

function readJpegDimensions(bytes: Uint8Array): ImageDimensions {
  if (bytes.length < 4 || bytes[0] !== 0xff || bytes[1] !== 0xd8) {
    throw new Error('Invalid JPEG image.');
  }

  let offset = 2;
  while (offset + 1 < bytes.length) {
    while (offset < bytes.length && bytes[offset] !== 0xff) {
      offset++;
    }
    while (offset < bytes.length && bytes[offset] === 0xff) {
      offset++;
    }
    if (offset >= bytes.length) {
      break;
    }

    const marker = bytes[offset++];
    if (marker === 0x00 || marker === 0x01 || (marker >= 0xd0 && marker <= 0xd7)) {
      continue;
    }
    if (marker === 0xd9 || marker === 0xda || offset + 1 >= bytes.length) {
      break;
    }

    const segmentLength = readUint16BigEndian(bytes, offset);
    if (segmentLength < 2 || offset + segmentLength > bytes.length) {
      break;
    }

    if (jpegStartOfFrameMarkers.has(marker)) {
      if (segmentLength < 7) {
        break;
      }
      return validDimensions(
        readUint16BigEndian(bytes, offset + 5),
        readUint16BigEndian(bytes, offset + 3)
      );
    }

    offset += segmentLength;
  }

  throw new Error('JPEG dimensions could not be read.');
}

function readPngDimensions(bytes: Uint8Array): ImageDimensions {
  const signature = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a];
  if (
    bytes.length < 24
    || !signature.every((value, index) => bytes[index] === value)
    || ascii(bytes, 12, 4) !== 'IHDR'
  ) {
    throw new Error('Invalid PNG image.');
  }

  return validDimensions(readUint32BigEndian(bytes, 16), readUint32BigEndian(bytes, 20));
}

function readWebpDimensions(bytes: Uint8Array): ImageDimensions {
  if (bytes.length < 30 || ascii(bytes, 0, 4) !== 'RIFF' || ascii(bytes, 8, 4) !== 'WEBP') {
    throw new Error('Invalid WebP image.');
  }

  const chunkType = ascii(bytes, 12, 4);
  if (chunkType === 'VP8X') {
    return validDimensions(
      1 + readUint24LittleEndian(bytes, 24),
      1 + readUint24LittleEndian(bytes, 27)
    );
  }

  if (chunkType === 'VP8L' && bytes[20] === 0x2f) {
    const first = bytes[21];
    const second = bytes[22];
    const third = bytes[23];
    const fourth = bytes[24];
    return validDimensions(
      1 + first + ((second & 0x3f) << 8),
      1 + (second >> 6) + (third << 2) + ((fourth & 0x0f) << 10)
    );
  }

  if (
    chunkType === 'VP8 '
    && bytes[23] === 0x9d
    && bytes[24] === 0x01
    && bytes[25] === 0x2a
  ) {
    return validDimensions(
      (bytes[26] | (bytes[27] << 8)) & 0x3fff,
      (bytes[28] | (bytes[29] << 8)) & 0x3fff
    );
  }

  throw new Error('WebP dimensions could not be read.');
}

function validDimensions(width: number, height: number): ImageDimensions {
  if (!Number.isSafeInteger(width) || !Number.isSafeInteger(height) || width <= 0 || height <= 0) {
    throw new Error('Image dimensions are invalid.');
  }
  return { width, height };
}

function readUint16BigEndian(bytes: Uint8Array, offset: number): number {
  return (bytes[offset] << 8) | bytes[offset + 1];
}

function readUint24LittleEndian(bytes: Uint8Array, offset: number): number {
  return bytes[offset] | (bytes[offset + 1] << 8) | (bytes[offset + 2] << 16);
}

function readUint32BigEndian(bytes: Uint8Array, offset: number): number {
  return (
    (bytes[offset] * 0x1000000)
    + (bytes[offset + 1] << 16)
    + (bytes[offset + 2] << 8)
    + bytes[offset + 3]
  );
}

function ascii(bytes: Uint8Array, offset: number, length: number): string {
  return String.fromCharCode(...bytes.subarray(offset, offset + length));
}
