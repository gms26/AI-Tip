const fs = require('fs');
const path = require('path');

const correctData = {
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
    if (correctData[file]) {
        const filePath = path.join(apiDir, file);
        let content = fs.readFileSync(filePath, 'utf8');
        
        // Find line with 'localhost:8080' and replace it completely
        const lines = content.split('\n');
        for (let i = 0; i < lines.length; i++) {
            if (lines[i].includes('localhost:8080')) {
                lines[i] = correctData[file];
            }
        }
        fs.writeFileSync(filePath, lines.join('\n'), 'utf8');
        console.log('Fixed', file);
    }
}
