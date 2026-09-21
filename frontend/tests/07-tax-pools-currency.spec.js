import { test, expect } from '@playwright/test';

test.describe('Tax, Pools, Currency', () => {
  const testUser = {
    name: 'Advanced User',
    email: `adv_user_${Date.now() + Math.random().toString(36).substring(7)}@test.com`,
    password: 'Password123!',
  };

  test.beforeAll(async ({ browser }) => {
    // Setup a user
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
    // Login
    await page.goto('/login');
    await page.fill('input[name="email"]', testUser.email);
    await page.fill('input[name="password"]', testUser.password);
    await page.click('button[type="submit"]');
    await page.waitForURL(/\/dashboard|\/tips/);
  });

  test('tip pool creation', async ({ page }) => {
    // Check if Tip Pools exists in navigation, else go directly
    await page.goto('/tip-pools');

    // Verify Title
    await expect(page.locator('text=Create Tip Pool').first()).toBeVisible();

    // Fill pool details using the actual UI labels
    await page.fill('label:has-text("Total Tip Amount") >> .. >> input', '200');
    await page.fill('label:has-text("Restaurant Name") >> .. >> input', 'Weekend Shift Pool');
    
    // We will verify the UI doesn't crash
    await expect(page.locator('text=Team Members').first()).toBeVisible();
  });
});
