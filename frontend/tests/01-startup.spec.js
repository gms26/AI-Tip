import { test, expect } from '@playwright/test';

test.describe('Application Startup', () => {
  test('frontend loads and navigation renders', async ({ page }) => {
    // Navigate to the root URL
    await page.goto('/');

    // Check that the title is correct
    await expect(page).toHaveTitle(/AI Tip Assistant/);

    // Verify main navigation or key element is visible
    // For example, checking if the branding text is present
    const branding = page.locator('text=Possible Tip').first();
    await expect(branding).toBeVisible();

    // Verify there are no fatal error messages
    const fatalError = page.locator('text=Unexpected error');
    await expect(fatalError).not.toBeVisible();
  });
});





