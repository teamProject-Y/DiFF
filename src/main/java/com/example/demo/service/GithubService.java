//package com.example.demo.service;
//
//import org.apache.http.HttpHeaders;
//import org.springframework.stereotype.Service;
//import org.springframework.web.reactive.function.client.WebClient;
//
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
//@Service
//public class GithubService {
//    private final WebClient gh = WebClient.builder()
//            .baseUrl("https://api.github.com")
//            .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
//            .build();
//
//    public RepoPage listUserRepos(String token, String affiliation, String visibility,
//                                  String sort, String direction, int page, int perPage, String q) {
//        return gh.get()
//                .uri(uri -> uri.path("/user/repos")
//                        .queryParam("affiliation", affiliation)   // owner,collaborator,organization_member
//                        .queryParam("visibility", visibility)     // all|public|private
//                        .queryParam("sort", sort)                 // created|updated|pushed|full_name
//                        .queryParam("direction", direction)       // asc|desc
//                        .queryParam("per_page", perPage)          // <= 100
//                        .queryParam("page", page)
//                        .build())
//                .headers(h -> h.setBearerAuth(token))
//                .exchangeToMono(resp -> resp.toEntityList(Map.class))
//                .map(entity -> {
//                    List<Map<String,Object>> body = entity.getBody();
//                    String linkHeader = Optional.ofNullable(entity.getHeaders().getFirst("Link")).orElse("");
//                    PageLinks links = PageLinks.parse(linkHeader); // next/prev/last/first 파싱
//
//                    // 필요한 필드만 추출 & 서버 측 검색어(q) 필터 (간단)
//                    List<RepoItem> items = body.stream()
//                            .map(GithubService::toRepoItem)
//                            .filter(r -> q == null || r.fullName().toLowerCase().contains(q.toLowerCase()))
//                            .toList();
//
//                    return new RepoPage(items, links.nextPage, links.prevPage, links.lastPage, links.firstPage, page);
//                }).block();
//    }
//
//    static RepoItem toRepoItem(Map<String,Object> m) {
//        Map<?,?> owner = (Map<?,?>) m.get("owner");
//        Map<?,?> perms = (Map<?,?>) m.get("permissions");
//        return new RepoItem(
//                (Integer) m.get("id"),
//                (String) m.get("name"),
//                (String) m.get("full_name"),
//                (String) owner.get("login"),
//                (Boolean) m.get("private"),
//                (String) m.get("default_branch"),
//                (String) m.get("html_url"),
//                (String) m.get("ssh_url"),
//                perms != null && Boolean.TRUE.equals(perms.get("admin")),
//                perms != null && Boolean.TRUE.equals(perms.get("push")),
//                perms != null && Boolean.TRUE.equals(perms.get("pull")),
//                (String) m.get("updated_at")
//        );
//    }
//
//    // Link 헤더 파서 (rel="next|prev|first|last")
//    static class PageLinks {
//        Integer nextPage, prevPage, firstPage, lastPage;
//        static PageLinks parse(String link) {
//            PageLinks pl = new PageLinks();
//            if (link == null) return pl;
//            for (String part : link.split(",\\s*")) {
//                // <https://api.github.com/user/repos?page=2&per_page=100>; rel="next"
//                int urlStart = part.indexOf('<'), urlEnd = part.indexOf('>');
//                int relStart = part.indexOf("rel=\"");
//                if (urlStart < 0 || urlEnd < 0 || relStart < 0) continue;
//                String url = part.substring(urlStart+1, urlEnd);
//                String rel = part.substring(relStart+5, part.indexOf('"', relStart+5));
//                Integer page = extractIntQuery(url, "page");
//                switch (rel) {
//                    case "next" -> pl.nextPage = page;
//                    case "prev" -> pl.prevPage = page;
//                    case "first" -> pl.firstPage = page;
//                    case "last" -> pl.lastPage = page;
//                }
//            }
//            return pl;
//        }
//        static Integer extractIntQuery(String url, String key) {
//            int i = url.indexOf('?'); if (i < 0) return null;
//            for (String q : url.substring(i+1).split("&")) {
//                String[] kv = q.split("=");
//                if (kv.length == 2 && kv[0].equals(key)) return Integer.valueOf(kv[1]);
//            }
//            return null;
//        }
//    }
//}
