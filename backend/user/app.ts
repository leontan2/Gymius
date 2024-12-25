import express from "express";
import mongoose from "mongoose";
import bodyParser from "body-parser";
import cors from "cors";

const app = express();

// Middleware
app.use(cors());
app.use(bodyParser.json());

app.get('/', (req: express.Request, res: express.Response, next: express.NextFunction) => {
  console.log("Sending Greetings!");
  res.json({
    message: "Hello World from Gymius-user!"
  });
});

// MongoDB connection
const connectToMongoDB = async (uri: string, type: string) => {
  try {
    await mongoose.connect(uri);
    console.log("Connected to MongoDB " + type + " successfully!");
  } catch (error) {
    console.error("MongoDB " + type + " connection error:", error);
  }
};

export { app, connectToMongoDB };