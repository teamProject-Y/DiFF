// next.config.js
const path = require('path')

module.exports = {
    // 1. 리라이팅 설정
    async redirects() {
        return [
            {
                source: '/login/github',
                destination: 'http://localhost:8080/oauth2/authorization/github',
                permanent: false
            },
            {
                source: '/login/google',
                destination: 'http://localhost:8080/oauth2/authorization/google',
                permanent: false
            },
            {
                source: '/api/:path*',
                destination: 'http://localhost:8080/usr/:path*',
                permanent: false
            }
        ]
    },

    // 2. 커스텀 CSS 로더 설정
    webpack(config) {
        config.module.rules.forEach(rule => {
            if (Array.isArray(rule.oneOf)) {
                rule.oneOf.unshift({
                    test: /\.css$/,
                    include: path.resolve(__dirname, 'src/resources/static/resource'),
                    use: ['style-loader', 'css-loader'],
                })
            }
        })
        return config
    }
}