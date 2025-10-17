const http = require('http');

async function testScriptExecution() {
  console.log('🔧 Testing BBPad Script Execution Features...\n');

  // Test scripts with various complexities
  const testScripts = [
    {
      name: 'Simple arithmetic',
      code: '(+ 1 2 3 4 5)',
      expectedType: 'number'
    },
    {
      name: 'String manipulation',
      code: '(str "Hello" " " "BBPad" " " "!" )',
      expectedType: 'string'
    },
    {
      name: 'List operations',
      code: '(map inc [1 2 3 4])',
      expectedType: 'list'
    },
    {
      name: 'Function definition',
      code: '(defn square [x] (* x x)) (square 5)',
      expectedType: 'number'
    },
    {
      name: 'Java interop',
      code: '(.toUpperCase "hello world")',
      expectedType: 'string'
    },
    {
      name: 'Date/time operations',
      code: '(str "Current time: " (java.time.LocalDateTime/now))',
      expectedType: 'string'
    },
    {
      name: 'Error handling test',
      code: '(+ 1 "invalid")',
      shouldError: true
    },
    {
      name: 'Complex data structure',
      code: '{:name "BBPad" :version "0.1.0" :features ["scripting" "database" "ui"]}',
      expectedType: 'object'
    }
  ];

  for (const test of testScripts) {
    console.log(`Testing: ${test.name}`);
    console.log(`Code: ${test.code}`);

    try {
      const result = await makeRequest('/api/execute', 'POST', { code: test.code });

      if (result.statusCode === 200) {
        const response = JSON.parse(result.body);
        console.log(`✅ Success: ${JSON.stringify(response).substring(0, 100)}...`);

        if (test.shouldError) {
          console.log('⚠️  Expected error but got success');
        }
      } else {
        console.log(`HTTP ${result.statusCode}: ${result.body}`);

        if (test.shouldError) {
          console.log('✅ Expected error occurred');
        } else {
          console.log('❌ Unexpected error');
        }
      }
    } catch (error) {
      console.log(`❌ Request failed: ${error.message}`);
    }

    console.log(''); // Empty line for readability
  }

  console.log('🔧 Testing database connectivity features...');

  // Test database driver availability
  try {
    const dbResponse = await makeRequest('/api/database/drivers');
    if (dbResponse.statusCode === 200) {
      const dbData = JSON.parse(dbResponse.body);
      console.log('✅ Database drivers:', JSON.stringify(dbData));
    } else {
      console.log('ℹ️  Database API not yet implemented');
    }
  } catch (error) {
    console.log('ℹ️  Database features not yet available');
  }

  console.log('\n🔧 Testing pod management...');

  // Test pod information
  try {
    const podResponse = await makeRequest('/api/pods');
    if (podResponse.statusCode === 200) {
      const podData = JSON.parse(podResponse);
      console.log('✅ Pod information:', JSON.stringify(podData));
    } else {
      console.log('ℹ️  Pod management API not yet implemented');
    }
  } catch (error) {
    console.log('ℹ️  Pod management features not yet available');
  }

  console.log('\n🎉 Script execution testing completed!');
}

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

// Run the tests
testScriptExecution().catch(console.error);