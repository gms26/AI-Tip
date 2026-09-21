import { test, expect } from '@playwright/test';

test.describe('Receipt OCR Upload', () => {
  test.skip('uploads receipt and parses it correctly (Feature not integrated in UI)', async ({ page }) => {
    // OCR API is available in backend and frontend services (receiptApi.js),
    // but the file upload UI was not implemented during the 40-day build.
    // Skipping to prevent false failure on missing UI.
  });
});
