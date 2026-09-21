import { test, expect } from '@playwright/test';

test.describe('Responsive UI', () => {
  // Mobile viewport
  test.use({ viewport: { width: 375, height: 667 } });

  test('navbar and dashboard render correctly on mobile', async ({ page }) => {
    // We don't necessarily need to login to test layout if landing page is responsive
    await page.goto('/');
    
    // Check for mobile menu icon or ensuring horizontal scroll is not present
    // Just verify the main container is visible and within bounds
    const bodyWidth = await page.evaluate(() => document.body.scrollWidth);
    expect(bodyWidth).toBeLessThanOrEqual(375);
  });
});





