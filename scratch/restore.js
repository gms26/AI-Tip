const fs = require('fs');
const path = require('path');

const restoreData = {
    'tipTimingApi.js': "const BASE_URL = import.meta.env.VITE_API_BASE_URL ? import.meta.env.VITE_API_BASE_URL + '/api/tip-timing' : 'http://localhost:8080/api/tip-timing';",
    'tipScenarioApi.js': "  baseURL: import.meta.env.VITE_API_BASE_URL ? import.meta.env.VITE_API_BASE_URL + '/api/tip-scenarios' : 'http://localhost:8080/api/tip-scenarios',",
    'tipHistoryApi.js': "const BASE_URL = import.meta.env.VITE_API_BASE_URL ? import.meta.env.VITE_API_BASE_URL + '/api/tips/history' : 'http://localhost:8080/api/tips/history';",
    'tipForecastApi.js': "  baseURL: import.meta.env.VITE_API_BASE_URL ? import.meta.env.VITE_API_BASE_URL + '/api/tip-forecast' : 'http://localhost:8080/api/tip-forecast',",
    'taxApi.js': "const BASE_URL = import.meta.env.VITE_API_BASE_URL ? import.meta.env.VITE_API_BASE_URL + '/api/tax' : 'http://localhost:8080/api/tax';",
    'posApi.js': "const API_URL = import.meta.env.VITE_API_BASE_URL ? import.meta.env.VITE_API_BASE_URL + '/api/pos' : 'http://localhost:8080/api/pos';",
    'paymentApi.js': "const API_URL = import.meta.env.VITE_API_BASE_URL ? import.meta.env.VITE_API_BASE_URL + '/api/payments' : 'http://localhost:8080/api/payments';",
    'goalApi.js': "  baseURL: import.meta.env.VITE_API_BASE_URL ? import.meta.env.VITE_API_BASE_URL + '/api/tip-goals' : 'http://localhost:8080/api/tip-goals',",
    'dataQualityApi.js': "  baseURL: import.meta.env.VITE_API_BASE_URL ? import.meta.env.VITE_API_BASE_URL + '/api/tips/data-quality' : 'http://localhost:8080/api/tips/data-quality',",
    'axios.js': "const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';"
};

const apiDir = path.join('d:', 'Project', 'AI Tip', 'frontend', 'src', 'api');
const files = fs.readdirSync(apiDir).filter(f => f.endsWith('.js'));

for (const file of files) {
    if (restoreData[file]) {
        const filePath = path.join(apiDir, file);
        let content = fs.readFileSync(filePath, 'utf8');
        
        // Find the broken line and replace it
        // The broken line starts with `const BASE_URL = (import` or `  baseURL: (import` or `const API_URL = (import` or `const API_BASE_URL = (import`
        content = content.replace(/.*\(import\.meta\.env\.VITE_API_BASE_URL \|\| \(import\.meta\.env\.VITE_API_BASE_URL \|\| 'http:\/\/localhost:8080'\) \+ ''\) \+ ''.*/, restoreData[file]);
        
        // Also handle the case where it might only be broken once (the powershell one)
        content = content.replace(/.*\(import\.meta\.env\.VITE_API_BASE_URL \|\| 'http:\/\/localhost:8080'\) \+ ''.*/, restoreData[file]);
        
        fs.writeFileSync(filePath, content, 'utf8');
        console.log('Restored', file);
    }
}
