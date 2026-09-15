const fs = require('fs');
const path = require('path');

const apiDir = path.join('d:', 'Project', 'AI Tip', 'frontend', 'src', 'api');
const files = fs.readdirSync(apiDir).filter(f => f.endsWith('.js') && f !== 'axios.js');

for (const file of files) {
    const filePath = path.join(apiDir, file);
    let content = fs.readFileSync(filePath, 'utf8');
    
    // Replace 'http://localhost:8080/...' with `(import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080') + '/...'`
    content = content.replace(/'http:\/\/localhost:8080(.*?)'/g, "(import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080') + '$1'");
    
    fs.writeFileSync(filePath, content, 'utf8');
}
console.log('Replaced URLs in', files.length, 'files.');
