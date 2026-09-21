import { test, expect } from '@playwright/test';

test.describe('Error States', () => {
  test('handles invalid route gracefully', async ({ page }) => {
    await page.goto('/invalid-route-that-does-not-exist');
    
    // Assuming there's a NotFound page
    await expect(page.locator('text=Page Not Found')).toBeVisible();
    await page.click('text=Go to Dashboard');
    
    // It might redirect to login if not authenticated
    await page.waitForURL(/\/dashboard|\/login/);
  });
});
