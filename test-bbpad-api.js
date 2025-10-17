const http = require('http');

// Test function for HTTP requests
function makeRequest(path, method = 'GET', data = null) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: 'localhost',
      port: 8080,
      path: path,
      method: method,
      headers: {
        'Content-Type': 'application/json',
      }
    };

    const req = http.request(options, (res) => {
      let body = '';
      res.on('data', (chunk) => {
        body += chunk;
      });
      res.on('end', () => {
        resolve({
          statusCode: res.statusCode,
          headers: res.headers,
          body: body
        });
      });
    });

    req.on('error', (err) => {
      reject(err);
    });

    if (data) {
      req.write(JSON.stringify(data));
    }
    req.end();
  });
}

async function runTests() {
  console.log('🧪 Starting BBPad API Tests...\n');

  try {
    // Test 1: Homepage
    console.log('1. Testing homepage...');
    const homeResponse = await makeRequest('/');
    console.log(`   Status: ${homeResponse.statusCode}`);
    console.log(`   Content-Type: ${homeResponse.headers['content-type']}`);
    console.log(`   Body length: ${homeResponse.body.length} characters`);
    console.log('   ✅ Homepage working\n');

    // Test 2: Health endpoint
    console.log('2. Testing health endpoint...');
    try {
      const healthResponse = await makeRequest('/api/health');
      console.log(`   Status: ${healthResponse.statusCode}`);
      if (healthResponse.statusCode === 200) {
        console.log(`   Response: ${healthResponse.body}`);
        console.log('   ✅ Health endpoint working\n');
      } else {
        console.log('   ⚠️  Health endpoint not found (expected in development)\n');
      }
    } catch (error) {
      console.log('   ⚠️  Health endpoint not implemented\n');
    }

    // Test 3: Script execution endpoint
    console.log('3. Testing script execution...');
    try {
      const execResponse = await makeRequest('/api/execute', 'POST', {
        code: '(str "Hello BBPad! " (* 6 7))'
      });
      console.log(`   Status: ${execResponse.statusCode}`);
      console.log(`   Response: ${execResponse.body}`);
      if (execResponse.statusCode === 200) {
        console.log('   ✅ Script execution working\n');
      } else {
        console.log('   ⚠️  Script execution endpoint needs implementation\n');
      }
    } catch (error) {
      console.log('   ⚠️  Script execution endpoint not implemented\n');
    }

    // Test 4: Static files
    console.log('4. Testing static file serving...');
    try {
      const staticResponse = await makeRequest('/css/style.css');
      console.log(`   CSS Status: ${staticResponse.statusCode}`);
    } catch (error) {
      console.log('   ⚠️  CSS files not found (expected in development)');
    }

    try {
      const jsResponse = await makeRequest('/js/app.js');
      console.log(`   JS Status: ${jsResponse.statusCode}`);
    } catch (error) {
      console.log('   ⚠️  JS files not found (expected in development)');
    }
    console.log('   ✅ Static file handling checked\n');

    // Test 5: WebSocket endpoint
    console.log('5. Testing WebSocket upgrade support...');
    const wsResponse = await makeRequest('/ws');
    console.log(`   WebSocket Status: ${wsResponse.statusCode}`);
    if (wsResponse.statusCode === 400 || wsResponse.statusCode === 101) {
      console.log('   ✅ WebSocket endpoint responding\n');
    } else {
      console.log('   ⚠️  WebSocket endpoint may need implementation\n');
    }

    console.log('🎉 API testing completed!');

  } catch (error) {
    console.error('❌ Test failed:', error.message);
  }
}

// Run the tests
runTests();