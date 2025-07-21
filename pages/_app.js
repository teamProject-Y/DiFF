// pages/_app.js
import Script from 'next/script'
import Layout from '../pages/common/layout'  // 네가 쓰던 그대로

export default function App({ Component, pageProps }) {
    return (
        <>
            {/* jQuery를 hydration 전에 불러오기 */}
            <Script
                src="https://cdnjs.cloudflare.com/ajax/libs/jquery/3.6.0/jquery.min.js"
                strategy="beforeInteractive"
            />

            {/* 레이아웃 + 페이지 컴포넌트 */}
            <Layout>
                <Component {...pageProps} />
            </Layout>
        </>
    )
}