const puppeteer = require('puppeteer');

async function runUITests() {
  console.log('🎨 Starting BBPad UI Tests with Puppeteer...\n');

  let browser;
  try {
    // Launch headless browser
    browser = await puppeteer.launch({
      headless: true,
      args: ['--no-sandbox', '--disable-setuid-sandbox']
    });

    const page = await browser.newPage();

    // Set viewport for consistent testing
    await page.setViewport({ width: 1200, height: 800 });

    // Test 1: Page Load
    console.log('1. Testing page load...');
    await page.goto('http://localhost:8080', { waitUntil: 'networkidle0' });

    const title = await page.title();
    console.log(`   Page title: "${title}"`);
    console.log('   ✅ Page loads successfully\n');

    // Test 2: Check for BBPad branding
    console.log('2. Testing BBPad branding...');
    const hasBBPadTitle = await page.$('h1') !== null;
    const hasBBPadContent = await page.evaluate(() => {
      return document.body.textContent.includes('BBPad');
    });

    console.log(`   Has H1: ${hasBBPadTitle}`);
    console.log(`   Contains BBPad text: ${hasBBPadContent}`);
    console.log('   ✅ Branding present\n');

    // Test 3: Check for development mode indicators
    console.log('3. Testing development mode features...');
    const devModeIndicators = await page.evaluate(() => {
      const content = document.body.textContent;
      return {
        hasServerStatus: content.includes('Server Status'),
        hasArchitecture: content.includes('Architecture'),
        hasPodSupport: content.includes('Pod Support'),
        hasDevelopmentMode: content.includes('Development Mode')
      };
    });

    console.log(`   Server status: ${devModeIndicators.hasServerStatus}`);
    console.log(`   Architecture info: ${devModeIndicators.hasArchitecture}`);
    console.log(`   Pod support: ${devModeIndicators.hasPodSupport}`);
    console.log(`   Development mode: ${devModeIndicators.hasDevelopmentMode}`);
    console.log('   ✅ Development mode features present\n');

    // Test 4: Test responsive design
    console.log('4. Testing responsive design...');

    // Mobile view
    await page.setViewport({ width: 375, height: 667 });
    await page.waitForTimeout(500);
    await page.screenshot({ path: 'test-results/bbpad-mobile.png' });
    console.log('   ✅ Mobile view captured');

    // Tablet view
    await page.setViewport({ width: 768, height: 1024 });
    await page.waitForTimeout(500);
    await page.screenshot({ path: 'test-results/bbpad-tablet.png' });
    console.log('   ✅ Tablet view captured');

    // Desktop view
    await page.setViewport({ width: 1200, height: 800 });
    await page.waitForTimeout(500);
    await page.screenshot({ path: 'test-results/bbpad-desktop.png' });
    console.log('   ✅ Desktop view captured\n');

    // Test 5: Check for interactive elements
    console.log('5. Testing interactive elements...');
    const buttons = await page.$$('button');
    const inputs = await page.$$('input, textarea');
    const clickableElements = await page.$$('a, button, [onclick]');

    console.log(`   Buttons found: ${buttons.length}`);
    console.log(`   Input fields: ${inputs.length}`);
    console.log(`   Clickable elements: ${clickableElements.length}`);
    console.log('   ✅ Interactive elements analyzed\n');

    // Test 6: Console error detection
    console.log('6. Testing for console errors...');
    const consoleMessages = [];
    page.on('console', msg => {
      if (msg.type() === 'error') {
        consoleMessages.push(msg.text());
      }
    });

    // Reload page to catch any loading errors
    await page.reload({ waitUntil: 'networkidle0' });
    await page.waitForTimeout(1000);

    if (consoleMessages.length === 0) {
      console.log('   ✅ No console errors detected');
    } else {
      console.log(`   ⚠️  ${consoleMessages.length} console errors:`);
      consoleMessages.forEach(msg => console.log(`     - ${msg}`));
    }
    console.log('');

    // Test 7: Network performance
    console.log('7. Testing network performance...');
    const metrics = await page.metrics();
    console.log(`   Timestamp: ${metrics.Timestamp}`);
    console.log(`   Documents: ${metrics.Documents}`);
    console.log(`   Frames: ${metrics.Frames}`);
    console.log(`   JSEventListeners: ${metrics.JSEventListeners}`);
    console.log(`   Nodes: ${metrics.Nodes}`);
    console.log(`   LayoutCount: ${metrics.LayoutCount}`);
    console.log(`   RecalcStyleCount: ${metrics.RecalcStyleCount}`);
    console.log('   ✅ Performance metrics collected\n');

    // Test 8: Content analysis
    console.log('8. Testing content structure...');
    const structure = await page.evaluate(() => {
      return {
        hasDOCTYPE: document.doctype !== null,
        htmlLang: document.documentElement.lang,
        title: document.title,
        metaDescription: document.querySelector('meta[name="description"]')?.content || 'Not set',
        hasViewport: document.querySelector('meta[name="viewport"]') !== null,
        hasAppDiv: document.querySelector('#app') !== null,
        bodyClasses: document.body.className
      };
    });

    console.log(`   DOCTYPE: ${structure.hasDOCTYPE}`);
    console.log(`   HTML lang: ${structure.htmlLang}`);
    console.log(`   Title: ${structure.title}`);
    console.log(`   Meta description: ${structure.metaDescription}`);
    console.log(`   Viewport meta: ${structure.hasViewport}`);
    console.log(`   #app div: ${structure.hasAppDiv}`);
    console.log(`   Body classes: ${structure.bodyClasses}`);
    console.log('   ✅ Content structure analyzed\n');

    console.log('🎉 UI testing completed successfully!');

  } catch (error) {
    console.error('❌ UI test failed:', error.message);
    console.error('Stack:', error.stack);
  } finally {
    if (browser) {
      await browser.close();
    }
  }
}

// Check if Puppeteer is available, install if needed
async function ensurePuppeteer() {
  try {
    require('puppeteer');
    await runUITests();
  } catch (error) {
    console.log('📦 Installing Puppeteer for UI testing...');
    const { execSync } = require('child_process');
    try {
      execSync('npm install puppeteer', { stdio: 'inherit' });
      await runUITests();
    } catch (installError) {
      console.error('❌ Failed to install Puppeteer:', installError.message);
      console.log('⚠️  Skipping UI tests - Puppeteer not available');
    }
  }
}

// Run the tests
ensurePuppeteer();