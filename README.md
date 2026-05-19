# Hızlı Okuma

Hızlı Okuma is a paired Android + Wear OS speed-reading app. You can select a PDF on your phone, extract its text, and send it to your watch for RSVP-style reading on the go.

## What it does

- Lets you pick a PDF from the phone companion app
- Extracts text from the PDF with PDFBox Android
- Sends the extracted text to the watch through the Wear OS data layer
- Displays the text on the watch in a speed-reading interface
- Supports reading controls such as play/pause, speed, font size, rewind, and progress tracking
- Includes watch tiles and complication support

## Project Structure

- `mobile/` - Phone companion app used to choose a PDF and send text to the watch
- `app/` - Wear OS app that renders the reader UI and related watch surfaces

## Requirements

- Android Studio installed
- A Wear OS watch or emulator for the `app` module
- An Android phone or emulator for the `mobile` module
- Google Play services available on the device or emulator for Wear OS data layer communication

## How It Works

1. Open the phone app.
2. Choose a PDF file.
3. The app extracts the PDF text locally on the phone.
4. The text is sent to the watch over the Wear OS data layer using the `/speed_reader_text` path.
5. The watch app loads the received text and starts speed reading.

## Build and Run

Open the project in Android Studio and run the modules you want to test.

From the command line, you can build both apps with:

```bash
./gradlew :app:assembleDebug :mobile:assembleDebug
```

To install and launch on connected devices or emulators, run the matching module from Android Studio:

- `mobile` for the phone companion app
- `app` for the Wear OS app

## App Details

### Phone Companion App

The phone app:

- Lets the user select a PDF file
- Uses PDFBox to extract the text content
- Sends the text to the watch with the Wearable Data Client

### Wear OS App

The watch app:

- Receives the text payload from the phone
- Shows a home screen with reader status
- Lets the user start reading, change speed, adjust font size, and reset progress
- Updates complication and tile providers when the app pauses

## Notes

- The current UI text is mostly in Turkish.
- The app is configured to be standalone on Wear OS, but the phone app is required to send PDFs to the watch.
- PDF files that contain scanned images instead of selectable text may not extract well without OCR.
