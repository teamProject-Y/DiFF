// pages/_document.js
import Document, { Html, Head, Main, NextScript } from 'next/document'
import Script from 'next/script'

export default class MyDocument extends Document {
    render() {
        return (
            <Html lang="ko">
                <Head>
                    {/* 백엔드 정적 리소스 */}
                    <link rel="stylesheet" href="/resource/common.css" />

                    {/* CDN 스타일 */}
                    <link
                        rel="stylesheet"
                        href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.7.2/css/all.min.css"
                    />
                    {/* daisyUI 스타일 불러오기 */}
                    <link
                        href="https://cdn.jsdelivr.net/npm/daisyui@5"
                        rel="stylesheet"
                        type="text/css"
                    />
                    {/* Tailwind 브라우저 플러그인 로드 (필수!) */}
                    <script
                        src="https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4"
                    ></script>
                    <link
                        rel="stylesheet"
                        href="https://cdnjs.cloudflare.com/ajax/libs/normalize/5.0.0/normalize.min.css"
                    />

                    {/* 스크립트는 next/script 로 */}
                    <Script src="https://cdn.tailwindcss.com" strategy="beforeInteractive" />
                    <Script
                        src="/resource/common.js"
                        strategy="afterInteractive"
                    />
                    <Script
                        src="https://cdnjs.cloudflare.com/ajax/libs/jquery/3.6.0/jquery.min.js"
                        strategy="afterInteractive"
                    />
                </Head>
                <body>
                <Main />
                <NextScript />
                </body>
            </Html>
        )
    }
}