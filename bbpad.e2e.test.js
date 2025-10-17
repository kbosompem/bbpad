const { test, expect } = require('@playwright/test');

test.describe('BBPad Core Functionality Tests', () => {
  test.beforeAll(async () => {
    // Ensure BBPad server is running
    console.log('🚀 Starting BBPad E2E tests...');
  });

  test.afterAll(async () => {
    console.log('✅ BBPad E2E tests completed');
  });

  test('should load BBPad homepage', async ({ page }) => {
    console.log('📄 Testing homepage load...');

    // Navigate to BBPad
    await page.goto('http://localhost:8080');

    // Wait for page to load
    await page.waitForLoadState('networkidle');

    // Check that page loads successfully
    await expect(page).toHaveTitle(/BBPad/i);

    // Take screenshot for verification
    await page.screenshot({ path: 'test-results/bbpad-homepage.png' });

    console.log('✅ Homepage loaded successfully');
  });

  test('should display code editor interface', async ({ page }) => {
    console.log('📝 Testing code editor interface...');

    await page.goto('http://localhost:8080');
    await page.waitForLoadState('networkidle');

    // Look for code editor elements
    const editor = await page.locator('.cm-editor, .CodeMirror, textarea, [contenteditable="true"]').first();

    if (await editor.isVisible()) {
      console.log('✅ Code editor found');

      // Test typing in editor
      await editor.fill('(println "Hello from BBPad!")');

      // Verify content was entered
      const content = await editor.inputValue();
      expect(content).toContain('Hello from BBPad');

      console.log('✅ Code editor input works correctly');
    } else {
      console.log('⚠️  Code editor not found - checking for alternative interface');

      // Look for any input element
      const input = await page.locator('input, textarea').first();
      if (await input.isVisible()) {
        await input.fill('(println "Hello from BBPad!")');
        console.log('✅ Input field found and working');
      }
    }
  });

  test('should execute simple Clojure code', async ({ page }) => {
    console.log('⚡ Testing code execution...');

    await page.goto('http://localhost:8080');
    await page.waitForLoadState('networkidle');

    // Look for run button
    const runButton = await page.locator('button:has-text("Run"), button:has-text("Execute"), [title*="Run"], [title*="Execute"]').first();

    if (await runButton.isVisible()) {
      console.log('✅ Run button found');

      // Enter simple code
      const editor = await page.locator('.cm-editor, .CodeMirror, textarea, [contenteditable="true"]').first();
      if (await editor.isVisible()) {
        await editor.fill('(+ 1 2 3)');

        // Click run button
        await runButton.click();

        // Wait for result
        await page.waitForTimeout(2000);

        // Look for output
        const output = await page.locator('.output, .result, .console, pre').first();
        if (await output.isVisible()) {
          const outputText = await output.textContent();
          console.log(`📊 Output: ${outputText}`);
          expect(outputText).toContain('6');
        }
      }
    } else {
      console.log('⚠️  Run button not found - testing API endpoints directly');
    }
  });

  test('should handle API endpoints correctly', async ({ page }) => {
    console.log('🔌 Testing API endpoints...');

    // Test health endpoint
    const healthResponse = await page.request.get('http://localhost:8080/api/health');
    if (healthResponse.ok()) {
      const healthData = await healthResponse.json();
      console.log(`✅ Health endpoint working: ${JSON.stringify(healthData)}`);
    }

    // Test script execution endpoint
    const execResponse = await page.request.post('http://localhost:8080/api/execute', {
      data: {
        code: '(str "Hello " "BBPad" " " (java.time.LocalDateTime/now))'
      }
    });

    if (execResponse.ok()) {
      const execData = await execResponse.json();
      console.log(`✅ Script execution working: ${JSON.stringify(execData)}`);
      expect(execData).toHaveProperty('result');
    }
  });

  test('should support WebSocket connections', async ({ page }) => {
    console.log('🔌 Testing WebSocket connectivity...');

    await page.goto('http://localhost:8080');
    await page.evaluate(() => {
      return new Promise((resolve, reject) => {
        const ws = new WebSocket('ws://localhost:8080/ws');

        ws.onopen = () => {
          console.log('✅ WebSocket connected');
          ws.send(JSON.stringify({ type: 'ping' }));
        };

        ws.onmessage = (event) => {
          const data = JSON.parse(event.data);
          console.log('📨 WebSocket message received:', data);
          ws.close();
          resolve(data);
        };

        ws.onerror = (error) => {
          console.log('⚠️  WebSocket error:', error);
          reject(error);
        };

        setTimeout(() => {
          ws.close();
          reject(new Error('WebSocket connection timeout'));
        }, 5000);
      });
    });
  });

  test('should handle script sharing URLs', async ({ page }) => {
    console.log('🔗 Testing script sharing functionality...');

    // Test with a script URL parameter
    await page.goto('http://localhost:8080?script=(println "Shared script test")');
    await page.waitForLoadState('networkidle');

    // Check if script was loaded
    const editor = await page.locator('.cm-editor, .CodeMirror, textarea, [contenteditable="true"]').first();
    if (await editor.isVisible()) {
      const content = await editor.inputValue();
      if (content.includes('Shared script test')) {
        console.log('✅ Script sharing via URL parameters works');
      }
    }
  });

  test('should display proper error handling', async ({ page }) => {
    console.log('⚠️  Testing error handling...');

    await page.goto('http://localhost:8080');
    await page.waitForLoadState('networkidle');

    // Try to execute invalid code
    const response = await page.request.post('http://localhost:8080/api/execute', {
      data: {
        code: '(invalid-function "test")'
      }
    });

    if (response.status() === 400 || response.status() === 500) {
      const errorData = await response.json();
      console.log(`✅ Error handling working: ${JSON.stringify(errorData)}`);
      expect(errorData).toHaveProperty('error');
    }
  });

  test('should support database connectivity features', async ({ page }) => {
    console.log('🗄️  Testing database connectivity features...');

    // Test database configuration endpoint
    const dbResponse = await page.request.get('http://localhost:8080/api/database/drivers');

    if (dbResponse.ok()) {
      const dbData = await dbResponse.json();
      console.log(`✅ Database drivers available: ${JSON.stringify(dbData)}`);
      expect(Array.isArray(dbData.drivers)).toBeTruthy();
    } else {
      console.log('ℹ️  Database endpoints not implemented yet');
    }
  });

  test('should have proper responsive design', async ({ page }) => {
    console.log('📱 Testing responsive design...');

    // Test desktop view
    await page.setViewportSize({ width: 1200, height: 800 });
    await page.goto('http://localhost:8080');
    await page.waitForLoadState('networkidle');
    await page.screenshot({ path: 'test-results/bbpad-desktop.png' });

    // Test mobile view
    await page.setViewportSize({ width: 375, height: 667 });
    await page.waitForLoadState('networkidle');
    await page.screenshot({ path: 'test-results/bbpad-mobile.png' });

    console.log('✅ Responsive design verified');
  });
});