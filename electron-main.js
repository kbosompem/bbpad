const { app, BrowserWindow, shell } = require('electron');
const { spawn } = require('child_process');
const path = require('path');
const net = require('net');

let mainWindow;
let bbpadProcess;
let serverPort;

// Function to find an available port
function findAvailablePort(startPort = 8080) {
  return new Promise((resolve) => {
    const server = net.createServer();
    server.listen(startPort, () => {
      const port = server.address().port;
      server.close(() => resolve(port));
    });
    server.on('error', () => {
      resolve(findAvailablePort(startPort + 1));
    });
  });
}

// Keep a global reference of the window object
async function createWindow() {
  // Find an available port
  serverPort = await findAvailablePort();
  console.log(`Using port ${serverPort} for BBPad server`);

  // Create the browser window
  mainWindow = new BrowserWindow({
    width: 1200,
    height: 800,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      enableRemoteModule: false,
      webSecurity: true
    },
    title: 'BBPad',
    autoHideMenuBar: true,
    titleBarStyle: 'default',
    icon: path.join(__dirname, 'assets', 'icon.png') // Add icon later
  });

  // Start BBPad server
  startBBPadServer();

  // Load the app after a short delay to ensure server is ready
  setTimeout(() => {
    mainWindow.loadURL(`http://localhost:${serverPort}`);
  }, 3000);

  // Handle external links
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url);
    return { action: 'deny' };
  });

  // Emitted when the window is closed
  mainWindow.on('closed', function () {
    // Dereference the window object
    mainWindow = null;
    // Kill BBPad server
    if (bbpadProcess) {
      bbpadProcess.kill('SIGTERM');
    }
  });

  // Development tools - only open DevTools if explicitly requested
  // if (process.env.NODE_ENV === 'development') {
  //   mainWindow.webContents.openDevTools();
  // }
}

function startBBPadServer() {
  console.log(`Starting BBPad server on port ${serverPort}...`);

  // Start the BBPad server with --no-webview flag and dynamic port
  bbpadProcess = spawn('bb', ['src/bbpad/main.clj', '--no-webview', '--port', serverPort.toString()], {
    cwd: __dirname,
    stdio: ['ignore', 'pipe', 'pipe']
  });

  bbpadProcess.stdout.on('data', (data) => {
    console.log(`BBPad: ${data}`);
  });

  bbpadProcess.stderr.on('data', (data) => {
    console.error(`BBPad Error: ${data}`);
  });

  bbpadProcess.on('close', (code) => {
    console.log(`BBPad server exited with code ${code}`);
  });

  bbpadProcess.on('error', (err) => {
    console.error('Failed to start BBPad server:', err);
  });
}

// This method will be called when Electron has finished initialization
app.whenReady().then(createWindow);

// Quit when all windows are closed
app.on('window-all-closed', function () {
  // Kill BBPad server
  if (bbpadProcess) {
    bbpadProcess.kill('SIGTERM');
  }

  // On macOS it is common for applications to stay open until explicitly quit
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', function () {
  // On macOS it's common to re-create a window when the dock icon is clicked
  if (mainWindow === null) {
    createWindow();
  }
});

// Security: Prevent navigation to external websites
app.on('web-contents-created', (event, contents) => {
  contents.on('will-navigate', (event, navigationUrl) => {
    const parsedUrl = new URL(navigationUrl);

    if (parsedUrl.origin !== `http://localhost:${serverPort}`) {
      event.preventDefault();
    }
  });
});