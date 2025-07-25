// 모바일에서 콘솔 로그를 화면에 표시

let debugDiv: HTMLDivElement | null = null;
const logs: string[] = [];

export const showMobileDebug = () => {
  if (debugDiv) return;
  
  debugDiv = document.createElement('div');
  debugDiv.style.cssText = `
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    height: 200px;
    background: rgba(0,0,0,0.9);
    color: #00ff00;
    font-size: 12px;
    padding: 10px;
    overflow-y: auto;
    z-index: 99999;
    font-family: monospace;
    white-space: pre-wrap;
  `;
  
  const closeBtn = document.createElement('button');
  closeBtn.textContent = '✕';
  closeBtn.style.cssText = `
    position: absolute;
    top: 5px;
    right: 5px;
    background: red;
    color: white;
    border: none;
    width: 20px;
    height: 20px;
    cursor: pointer;
  `;
  closeBtn.onclick = () => {
    document.body.removeChild(debugDiv!);
    debugDiv = null;
  };
  
  debugDiv.appendChild(closeBtn);
  document.body.appendChild(debugDiv);
  
  updateDebugDisplay();
};

const updateDebugDisplay = () => {
  if (!debugDiv) return;
  debugDiv.innerHTML = `<button style="position: absolute; top: 5px; right: 5px; background: red; color: white; border: none; width: 20px; height: 20px; cursor: pointer;" onclick="this.parentElement.remove()">✕</button>${logs.slice(-20).join('\n')}`;
};

export const mobileLog = (message: string) => {
  const timestamp = new Date().toLocaleTimeString();
  const logMessage = `[${timestamp}] ${message}`;
  logs.push(logMessage);
  console.log(logMessage);
  
  if (debugDiv) {
    updateDebugDisplay();
  }
};

// 기존 console 메서드 오버라이드
const originalLog = console.log;
const originalError = console.error;
const originalWarn = console.warn;

console.log = (...args) => {
  mobileLog(args.join(' '));
  originalLog.apply(console, args);
};

console.error = (...args) => {
  mobileLog('ERROR: ' + args.join(' '));
  originalError.apply(console, args);
};

console.warn = (...args) => {
  mobileLog('WARN: ' + args.join(' '));
  originalWarn.apply(console, args);
};