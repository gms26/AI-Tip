import puppeteer from 'puppeteer';
import fs from 'fs';
import path from 'path';

const ARTIFACT_DIR = 'C:\\Users\\SANJAY\\.gemini\\antigravity-ide\\brain\\5de0cb07-533f-41ea-b0c6-110bc6e41cad';

async function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

(async () => {
  console.log("Starting Puppeteer verification script...");
  const browser = await puppeteer.launch({ headless: "new" });
  const page = await browser.newPage();
  await page.setViewport({ width: 1280, height: 800 });

  try {
    // 1. Register a new user
    const randomUser = `test_${Date.now()}@tax.com`;
    console.log(`Registering new user: ${randomUser}`);
    await page.goto('http://localhost:5173/register', { waitUntil: 'networkidle2' });
    
    await page.type('input[placeholder*="Name"], input[name="name"], #name', 'Tax Tester');
    await page.type('input[placeholder*="Email"], input[name="email"], #email', randomUser);
    await page.type('input[placeholder*="Password"], input[name="password"], #password', 'password123');
    
    // Find and click the register/submit button
    const submitBtn = await page.$('button[type="submit"]');
    await submitBtn.click();
    
    // Wait for Dashboard to load
    await page.waitForNavigation({ waitUntil: 'networkidle2' });
    console.log("Registered and navigated to Dashboard.");
    
    // 2. Wait a moment for data to load
    await delay(1500);

    // 3. Verify Empty State (0 tips)
    console.log("Verifying empty state...");
    await page.screenshot({ path: path.join(ARTIFACT_DIR, 'tax_empty_state.png') });
    console.log("Captured empty state screenshot.");

    // 4. Add First Tip (USD)
    console.log("Navigating to Tips to add first tip...");
    await page.goto('http://localhost:5173/tips', { waitUntil: 'networkidle2' });
    
    await page.type('input[placeholder*="Bill"], input[type="number"]', '100');
    // Assuming there is a currency selector, defaults to USD or we select it
    const currencySelects = await page.$$('select');
    if (currencySelects.length > 0) {
       await currencySelects[0].select('USD');
    }

    // Try to find a 20% button or input
    const buttons = await page.$$('button');
    for (const btn of buttons) {
        const text = await page.evaluate(el => el.textContent, btn);
        if (text && text.includes('20%')) {
            await btn.click();
            break;
        }
    }
    
    const inputs = await page.$$('input[type="text"]');
    for (const input of inputs) {
        const placeholder = await page.evaluate(el => el.getAttribute('placeholder'), input);
        if (placeholder && (placeholder.toLowerCase().includes('restaurant') || placeholder.toLowerCase().includes('name'))) {
            await input.type('Steakhouse');
        }
    }

    // Save tip
    let saveClicked = false;
    for (const btn of buttons) {
        const text = await page.evaluate(el => el.textContent, btn);
        if (text && text.toLowerCase().includes('save')) {
            await btn.click();
            saveClicked = true;
            break;
        }
    }
    
    if(!saveClicked) {
      const submitBtn2 = await page.$('button[type="submit"]');
      if(submitBtn2) await submitBtn2.click();
    }

    await delay(2000);
    console.log("Saved USD tip.");

    // 5. Verify 50% assumption
    await page.goto('http://localhost:5173/dashboard', { waitUntil: 'networkidle2' });
    await delay(1500);
    
    // Change tax percentage to 50
    const taxInputs = await page.$$('input[type="range"], input[type="number"]');
    for (const input of taxInputs) {
        // Evaluate to see if it's the tax input
        // Just clear and type 50 for all number/range inputs for safety if there's only one
        await input.click();
        await page.keyboard.press('Backspace');
        await page.keyboard.press('Backspace');
        await input.type('50');
    }
    await delay(1000);
    await page.screenshot({ path: path.join(ARTIFACT_DIR, 'tax_single_currency_50.png') });
    console.log("Captured 50% assumption screenshot.");

    // 6. Add Second Tip (INR)
    console.log("Navigating to Tips to add INR tip...");
    await page.goto('http://localhost:5173/tips', { waitUntil: 'networkidle2' });
    
    // Clear bill amount if needed
    const billInputs = await page.$$('input[type="number"]');
    if(billInputs.length > 0) {
        await billInputs[0].click({clickCount: 3});
        await page.keyboard.press('Backspace');
        await billInputs[0].type('1000');
    }

    const currencySelects2 = await page.$$('select');
    if (currencySelects2.length > 0) {
       await currencySelects2[0].select('INR');
    }

    const buttons2 = await page.$$('button');
    for (const btn of buttons2) {
        const text = await page.evaluate(el => el.textContent, btn);
        if (text && text.includes('10%')) {
            await btn.click();
            break;
        }
    }
    
    const inputs2 = await page.$$('input[type="text"]');
    for (const input of inputs2) {
        const placeholder = await page.evaluate(el => el.getAttribute('placeholder'), input);
        if (placeholder && (placeholder.toLowerCase().includes('restaurant') || placeholder.toLowerCase().includes('name'))) {
            await input.click({clickCount: 3});
            await page.keyboard.press('Backspace');
            await input.type('Curry House');
        }
    }

    // Save tip
    saveClicked = false;
    for (const btn of buttons2) {
        const text = await page.evaluate(el => el.textContent, btn);
        if (text && text.toLowerCase().includes('save')) {
            await btn.click();
            saveClicked = true;
            break;
        }
    }
    if(!saveClicked) {
      const submitBtn3 = await page.$('button[type="submit"]');
      if(submitBtn3) await submitBtn3.click();
    }
    
    await delay(2000);
    console.log("Saved INR tip.");

    // 7. Verify Multi Currency
    await page.goto('http://localhost:5173/dashboard', { waitUntil: 'networkidle2' });
    await delay(2000);
    await page.screenshot({ path: path.join(ARTIFACT_DIR, 'tax_multi_currency.png') });
    console.log("Captured multiple currency screenshot.");
    
    console.log("Verification complete.");
  } catch (error) {
    console.error("Error during verification:", error);
  } finally {
    await browser.close();
  }
})();
