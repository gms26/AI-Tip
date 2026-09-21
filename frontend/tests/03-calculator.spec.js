import { test, expect } from '@playwright/test';

test.describe('Tip Calculator & Split', () => {
  const testUser = {
    name: 'Calc User',
    email: `calc_user_${Date.now() + Math.random().toString(36).substring(7)}@test.com`,
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
    await page.goto('/tips'); // Go to calculator
  });

  test('calculates tip correctly', async ({ page }) => {
    // We are on the Tip Calculator page
    // Fill bill amount
    await page.fill('input[placeholder="0.00"]', '100'); // Assuming bill amount has placeholder 0.00
    
    // It should automatically calculate a default tip (e.g. 15% or 18% or something depending on UI)
    // Let's explicitly select 20%
    await page.click('button:has-text("20%")');

    // Wait for calculations to update
    // Verify Tip Amount is $20.00 and Total is $120.00
    // We expect the text to appear somewhere on the page
    await expect(page.locator('text=$20.00').first()).toBeVisible();
    await expect(page.locator('text=$120.00').first()).toBeVisible();
  });

  test('splits tip evenly', async ({ page }) => {
    await page.fill('input[placeholder="0.00"]', '100');
    await page.click('button:has-text("20%")');
    
    // Toggle split section
    await page.getByText('Split Bill').click();
    await page.getByText('Equal Split').click();

    // The default is usually 2 people, we might need to click a '+' button if available
    // or just check that a split breakdown appears
    await expect(page.locator('text=Split Breakdown:')).toBeVisible();

    // Check if the individual amount owed is displayed (e.g. $60.00 each for 2 people)
    // We can just verify it shows up
    await expect(page.locator('text=$60.00').first()).toBeVisible();
  });
});
