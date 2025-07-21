// common/header.js
import Link from 'next/link'

export default function Header() {
    return (
        <header className="flex h-22 w-full p-4 m-2 text-neutral-600">
            <div className="logo pl-4">
                {/* 아이콘도 이렇게 */}
                <Link href="/" className="block px-6">
                    <i className="fa-solid fa-star"></i>
                </Link>
            </div>
            <div className="flex-grow" />
            <nav className="items-center mr-6 text-4xl text-neutral-800">
                <ul className="flex">
                    <li className="hover:underline hover:text-neutral-400">
                        <Link href="/" className="block px-6">
                            HOME
                        </Link>
                    </li>
                    <li className="hover:underline hover:text-neutral-400">
                        <Link href="/home/faq" className="block px-6">
                            FAQ
                        </Link>
                    </li>
                    <li className="relative group">
                        <Link href="/article/list" className="block px-6">
                            LIST
                        </Link>
                        <ul className="absolute hidden group-hover:block w-full top-full py-5 text-center text-lg whitespace-nowrap bg-white shadow-lg">
                            {['전체 게시판','공지사항','자유 게시판','QnA'].map((label, id) => (
                                <li key={id}>
                                    <Link
                                        href={`/article/list?boardId=${id}`}
                                        className="block h-full p-1 hover:underline hover:text-neutral-400"
                                    >
                                        {label}
                                    </Link>
                                </li>
                            ))}
                        </ul>
                    </li>

                    {/* 로그인 훅 없이 고정 버튼 */}
                    <li className="hover:underline hover:text-neutral-400">
                        <Link href="/DiFF/member/login" className="block px-6">
                            LOGIN
                        </Link>
                    </li>
                    <li className="hover:underline hover:text-neutral-400">
                        <Link href="/DiFF/member/join" className="block px-6">
                            JOIN
                        </Link>
                    </li>
                </ul>
            </nav>
        </header>
    )
}