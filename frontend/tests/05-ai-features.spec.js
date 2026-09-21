import { test, expect } from '@playwright/test';

test.describe('AI & Smart Features', () => {
  const testUser = {
    name: 'AI User',
    email: `ai_user_${Date.now() + Math.random().toString(36).substring(7)}@test.com`,
    password: 'Password123!',
  };

  test.beforeAll(async ({ browser }) => {
    // Setup a user to use in the tests
    const page = await browser.newPage();
    await page.goto('/register');
    await page.fill('input[name="name"]', testUser.name);
    await page.fill('input[name="email"]', testUser.email);
    await page.fill('input[name="password"]', testUser.password);
    await page.fill('input[name="confirmPassword"]', testUser.password);
    await page.click('button[type="submit"]');
    await page.waitForURL(/\/dashboard|\/tips/);
    await page.close();
  });

  test.beforeEach(async ({ page }) => {
    // Login before each test
    await page.goto('/login');
    await page.fill('input[name="email"]', testUser.email);
    await page.fill('input[name="password"]', testUser.password);
    await page.click('button[type="submit"]');
    await page.waitForURL(/\/dashboard|\/tips/);
  });

  test('AI recommendation is displayed correctly', async ({ page }) => {
    // Intercept the AI suggestion API to avoid hitting Groq unnecessarily
    await page.route('**/api/ai/suggest', async route => {
      const mockResponse = {
        recommendedPercentage: 18.5,
        minimumPercentage: 15.0,
        maximumPercentage: 22.0,
        tipAmount: 18.5,
        totalAmount: 118.5,
        reason: 'Mock AI reason for testing.',
        confidence: 'HIGH',
        source: 'OVERALL_AVERAGE'
      };
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(mockResponse)
      });
    });

    await page.goto('/tips'); // The AI suggestion happens on Tip Calculator

    await page.fill('input[placeholder="0.00"]', '100');
    // Wait for button to be enabled (requires billAmount > 0)
    await page.click('button:has-text("Get AI Suggestion")'); 

    // Verify UI reflects the AI response
    // When aiResult is set, it updates the main tip percentage, which should show 18.5% somewhere, and also applied recommendation snackbar
    await expect(page.locator('text=18.5').first()).toBeVisible();
  });
});
