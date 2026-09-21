import { test, expect } from '@playwright/test';

test.describe('Authentication Flow', () => {
  const testUser = {
    name: 'E2E User',
    email: `e2e_user_${Date.now() + Math.random().toString(36).substring(7)}@test.com`,
    password: 'Password123!',
  };

  test('user can register, login, access protected route, and logout', async ({ page }) => {
    // Navigate directly to register page
    await page.goto('/register');
    
    // Fill out registration form
    await page.fill('input[name="name"]', testUser.name);
    await page.fill('input[name="email"]', testUser.email);
    await page.fill('input[name="password"]', testUser.password);
    await page.fill('input[name="confirmPassword"]', testUser.password);
    await page.click('button[type="submit"]');

    // Wait for redirect to dashboard (Registration auto-logs in)
    await page.waitForURL(/\/dashboard|\/tips/);
    
    // Verify dashboard content is visible
    await expect(page.locator('text=Welcome').first()).toBeVisible(); 
    
    // Now let's test logout
    await page.click('button[aria-label="Account settings"], button[title="Account settings"], button:has(.MuiAvatar-root)');
    await page.click('text=Sign Out');

    // Verify redirected to login (or landing)
    await page.waitForURL(/\/login|\//);
    await expect(page.locator('text=Sign In').first()).toBeVisible();
    
    // Now test login with the registered credentials
    await page.goto('/login');
    await page.fill('input[name="email"]', testUser.email);
    await page.fill('input[name="password"]', testUser.password);
    await page.click('button[type="submit"]');

    // Verify successful login
    await page.waitForURL(/\/dashboard|\/tips/);
    await expect(page.locator('text=Welcome').first()).toBeVisible();
  });

  test('invalid login is rejected', async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[name="email"]', 'wrong@test.com');
    await page.fill('input[name="password"]', 'wrongpassword');
    await page.click('button[type="submit"]');

    // Error message should appear
    await expect(page.locator('text=Invalid email or password')).toBeVisible();
  });
});
