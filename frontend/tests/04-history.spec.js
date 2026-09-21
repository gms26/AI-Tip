import { test, expect } from '@playwright/test';

test.describe('Tip History', () => {
  const testUser = {
    name: 'History User',
    email: `history_user_${Date.now() + Math.random().toString(36).substring(7)}@test.com`,
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

  test('saves a tip and views it in history', async ({ page }) => {
    // 1. Go to Tip Calculator
    await page.goto('/tips');
    
    // 2. Fill out a tip and save it
    await page.fill('input[placeholder="0.00"]', '100');
    await page.click('button:has-text("20%")');
    
    // Fill the actual restaurant name input using its label
    await page.fill('label:has-text("Restaurant") >> .. >> input', 'Test Restaurant');
    await page.click('button:has-text("Save Tip")'); // Adjust text if needed
    
    // Check for success message
    await expect(page.locator('text=Tip saved successfully')).toBeVisible();

    // 3. Go to Tip History
    await page.goto('/tip-history');

    // 4. Verify the tip is listed
    await expect(page.locator('text=Test Restaurant').first()).toBeVisible();
  });
});
