import java.util.*;

public class Main {
    private static String NICKNAME = "서울10_이정헌";
    private static String[][] mapData;  // 맵 정보
    private static Map<String, String[]> allies  = new HashMap<>();  // 아군 정보
    private static Map<String, String[]> enemies = new HashMap<>();  // 적군 정보
    private static String[] codes;  // 암호문 정보

    // 방향 배열 (U·D·L·R 순서)
    static final int[]    dr  = {-1,  1,  0, 0};
    static final int[]    dc  = { 0,  0, -1, 1};
    static final String[] DIR = {"U", "D", "L", "R"};

    static final int SHOOT_RANGE = 3;   // 포탄 최대 사거리 (칸)
    static final int MAX_SHELL   = 12;  // 다익스트라 탄약 상한 (메모리 안전 상한)

    // 같은 암호문 중복 제출 방지
    static final Set<String> decodedCodes = new HashSet<>();

    public static void main(String[] args) {
        Bridge bridge = new Bridge();
        String gameData = bridge.init(NICKNAME);

        // while 반복문: 배틀싸피 메인 프로그램과 클라이언트(이 코드)가 데이터를 계속해서 주고받는 부분
        while (gameData.length() > 0) {
            // 자기 차례가 되어 받은 게임정보를 파싱
            System.out.printf("----입력데이터----\n%s\n----------------\n", gameData);
            parseData(gameData);

            // 파싱한 데이터를 화면에 출력하여 확인
            System.out.printf("\n[맵 정보] (%d x %d)\n", mapData.length, mapData[0].length);
            for (int i = 0; i < mapData.length; i++) {
                for (int j = 0; j < mapData[i].length; j++) {
                    System.out.printf("%s ", mapData[i][j]);
                }
                System.out.println();
            }

            System.out.printf("\n[아군 정보] (아군 수: %d)\n", allies.size());
            for (String key : allies.keySet()) {
                String[] value = allies.get(key);
                if (key.equals("A")) {
                    System.out.printf("A (내 탱크) - 체력: %s, 방향: %s, 보유한 일반 포탄: %s개, 보유한 대전차 포탄: %s개\n",
                            value[0], value[1], value[2], value[3]);
                } else if (key.equals("H")) {
                    System.out.printf("H (아군 포탑) - 체력: %s\n", value[0]);
                } else {
                    System.out.printf("%s (아군 탱크) - 체력: %s\n", key, value[0]);
                }
            }

            System.out.printf("\n[적군 정보] (적군 수: %d)\n", enemies.size());
            for (String key : enemies.keySet()) {
                String[] value = enemies.get(key);
                if (key.equals("X")) {
                    System.out.printf("X (적군 포탑) - 체력: %s\n", value[0]);
                } else {
                    System.out.printf("%s (적군 탱크) - 체력: %s\n", key, value[0]);
                }
            }

            System.out.printf("\n[암호문 정보] (암호문 수: %d)\n", codes.length);
            for (int i = 0; i < codes.length; i++) {
                System.out.printf("%s\n", codes[i]);
            }

            // 탱크의 동작을 결정하기 위한 알고리즘을 구현하고 원하는 커맨드를 output 변수에 담기
            String output = solve();
            if (output == null || output.isEmpty()) output = "S";

            System.out.printf("[SEND] %s\n", output);

            // while 문의 끝에는 다음 코드가 필수로 존재하여야 함
            // output에 담긴 값은 submit 함수를 통해 배틀싸피 메인 프로그램에 전달
            gameData = bridge.submit(output);
        }

        // 반복문을 빠져나왔을 때 배틀싸피 메인 프로그램과의 연결을 완전히 해제하기 위해 close 함수 호출
        bridge.close();
    }


    // =============================================
    // 메인 전략 결정 함수
    // 우선순위: 즉시 사격 → 전투 경로 계획 → 보급 이동
    // =============================================
    private static String solve() {
        // 내 탱크 위치 및 적 포탑 위치 파악
        int[] me     = findPos("A");
        int[] target = findPos("X");
        if (me == null || target == null) return "S";

        // 내 탱크 정보(체력, 탄약) 읽기
        String[] myInfo = allies.get("A");
        if (myInfo == null || myInfo.length < 4) return "S";
        int hp          = toInt(myInfo[0], 100);
        int normalShell = toInt(myInfo[2], 0);
        int megaShell   = toInt(myInfo[3], 0);

        // 1) 즉시 사격: 적 포탑이 사거리 내 일직선에 있으면 바로 발사
        if (normalShell > 0) {
            String cmd = shootCmd(me, target, "F");
            if (cmd != null) return cmd;
        }
        if (megaShell > 0) {
            String cmd = shootCmd(me, target, "F M");
            if (cmd != null) return cmd;
        }

        // 2) 전투 경로 계획: 다익스트라로 포탑 사격 가능 위치까지 최적 이동 경로 계산
        String battleCmd = planBattle(me, target, hp, normalShell, megaShell);
        if (battleCmd != null) return battleCmd;

        // 3) 보급 이동: 전투 경로가 없을 때 보급소(F)로 이동하여 암호문 해독 후 탄약 보충
        if (codes.length > 0 && isNextToSupply(me)) {
            String code = codes[0];
            if (!decodedCodes.contains(code)) {
                decodedCodes.add(code);
                String plain = decodeCaesar(code);
                return "G " + plain;
            }
        }
        String supplyCmd = planSupply(me, hp, normalShell, megaShell);
        if (supplyCmd != null) return supplyCmd;

        return "S";  // 가능한 행동 없을 때 대기
    }


    // =============================================
    // 전투 경로 계획 (다익스트라)
    //
    // 상태: (행, 열, 일반탄 수, 대전차탄 수)
    // 비용: G 이동=1, 모래(S) 이동=2, 나무(T) 파괴+진입=2 (탄약 1 소모)
    //       W(물)·R(바위)·X(포탑)·E*(적 탱크) 등은 진입 불가
    // 목표: 적 포탑(X)을 사격 가능한 칸에 도달하고 탄약 ≥ 1
    // 반환: 현재 턴에 실행할 첫 번째 커맨드 (매 턴 재계산 방식)
    // =============================================
    private static String planBattle(int[] start, int[] target, int hp, int normalShell, int megaShell) {
        int H = mapData.length, W = mapData[0].length;
        // 메모리 안전 상한 적용 (탄약이 많을 때 배열 크기 폭증 방지)
        int N = Math.min(normalShell, MAX_SHELL);
        int M = Math.min(megaShell,   MAX_SHELL);

        int[][][][]    dist     = new int[H][W][N + 1][M + 1];
        String[][][][] firstCmd = new String[H][W][N + 1][M + 1];
        for (int r = 0; r < H; r++)
            for (int c = 0; c < W; c++)
                for (int n = 0; n <= N; n++)
                    Arrays.fill(dist[r][c][n], Integer.MAX_VALUE);

        // 우선순위 큐: [row, col, normalShell, megaShell, cost]
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[4]));
        dist[start[0]][start[1]][N][M] = 0;
        pq.offer(new int[]{start[0], start[1], N, M, 0});

        while (!pq.isEmpty()) {
            int[] cur = pq.poll();
            int r = cur[0], c = cur[1], n = cur[2], m = cur[3], cost = cur[4];
            if (cost > dist[r][c][n][m]) continue;  // 오래된 상태 무시

            // 목표 도달: 시작점이 아니고, 포탑 사격 가능, 탄약 ≥ 1
            if (!(r == start[0] && c == start[1]) && canShootFrom(r, c, target) && (n + m >= 1)) {
                return firstCmd[r][c][n][m];
            }

            for (int d = 0; d < 4; d++) {
                int nr = r + dr[d], nc = c + dc[d];
                if (!inBound(nr, nc)) continue;
                String cell = mapData[nr][nc];

                if (isMovable(cell)) {
                    // 체력이 낮을 때 모래 칸 진입 금지 (추가 데미지 방지)
                    if (isSand(cell) && hp <= 10) continue;
                    int add = isSand(cell) ? 2 : 1;
                    relax(dist, firstCmd, pq, r, c, n, m, cost,
                          nr, nc, n, m, cost + add, DIR[d] + " A", start, N, M);

                } else if (isTree(cell)) {
                    // 나무: 일반 포탄으로 파괴 후 진입
                    if (n > 0)
                        relax(dist, firstCmd, pq, r, c, n, m, cost,
                              nr, nc, n - 1, m, cost + 2, DIR[d] + " F", start, N, M);
                    // 나무: 대전차 포탄으로 파괴 후 진입
                    if (m > 0)
                        relax(dist, firstCmd, pq, r, c, n, m, cost,
                              nr, nc, n, m - 1, cost + 2, DIR[d] + " F M", start, N, M);
                }
                // 그 외(W·R·X·E* 등)은 진입 불가 → 탐색 제외
            }
        }
        return null;  // 도달 가능한 전투 위치 없음
    }


    // =============================================
    // 보급 경로 계획 (다익스트라)
    //
    // 목표: 보급소(F) 바로 옆 칸에 도달
    // 구조는 planBattle과 동일, 목표 조건만 다름
    // =============================================
    private static String planSupply(int[] start, int hp, int normalShell, int megaShell) {
        if (!hasSupplyOnMap()) return null;  // 보급소 없으면 탐색 불필요

        int H = mapData.length, W = mapData[0].length;
        int N = Math.min(normalShell, MAX_SHELL);
        int M = Math.min(megaShell,   MAX_SHELL);

        int[][][][]    dist     = new int[H][W][N + 1][M + 1];
        String[][][][] firstCmd = new String[H][W][N + 1][M + 1];
        for (int r = 0; r < H; r++)
            for (int c = 0; c < W; c++)
                for (int n = 0; n <= N; n++)
                    Arrays.fill(dist[r][c][n], Integer.MAX_VALUE);

        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[4]));
        dist[start[0]][start[1]][N][M] = 0;
        pq.offer(new int[]{start[0], start[1], N, M, 0});

        while (!pq.isEmpty()) {
            int[] cur = pq.poll();
            int r = cur[0], c = cur[1], n = cur[2], m = cur[3], cost = cur[4];
            if (cost > dist[r][c][n][m]) continue;

            // 목표 도달: 보급소 인접 칸 (시작점 제외)
            if (!(r == start[0] && c == start[1]) && isNextToSupply(new int[]{r, c})) {
                return firstCmd[r][c][n][m];
            }

            for (int d = 0; d < 4; d++) {
                int nr = r + dr[d], nc = c + dc[d];
                if (!inBound(nr, nc)) continue;
                String cell = mapData[nr][nc];

                if (isMovable(cell)) {
                    if (isSand(cell) && hp <= 10) continue;
                    int add = isSand(cell) ? 2 : 1;
                    relax(dist, firstCmd, pq, r, c, n, m, cost,
                          nr, nc, n, m, cost + add, DIR[d] + " A", start, N, M);
                } else if (isTree(cell)) {
                    if (n > 0)
                        relax(dist, firstCmd, pq, r, c, n, m, cost,
                              nr, nc, n - 1, m, cost + 2, DIR[d] + " F", start, N, M);
                    if (m > 0)
                        relax(dist, firstCmd, pq, r, c, n, m, cost,
                              nr, nc, n, m - 1, cost + 2, DIR[d] + " F M", start, N, M);
                }
            }
        }
        return null;
    }


    // =============================================
    // 다익스트라 릴랙싱 공통 헬퍼
    // planBattle·planSupply 양쪽에서 반복되는 릴랙싱 로직을 한 곳으로 통합
    // =============================================
    private static void relax(int[][][][] dist, String[][][][] firstCmd, PriorityQueue<int[]> pq,
                               int r,  int c,  int n,  int m,  int cost,
                               int nr, int nc, int nn, int nm, int nd,
                               String cmd, int[] start, int N, int M) {
        if (nn < 0 || nm < 0 || nn > N || nm > M) return;
        if (nd >= dist[nr][nc][nn][nm]) return;
        dist[nr][nc][nn][nm] = nd;
        // 시작점에서 바로 이동할 때는 현재 커맨드를 기록, 이후에는 부모의 첫 커맨드를 그대로 전달
        firstCmd[nr][nc][nn][nm] = (r == start[0] && c == start[1] && n == N && m == M)
                ? cmd : firstCmd[r][c][n][m];
        pq.offer(new int[]{nr, nc, nn, nm, nd});
    }


    // =============================================
    // 사격 커맨드 생성
    // 같은 행 또는 열에 있고 사거리(SHOOT_RANGE) 이내이며
    // 중간 경로에 장애물 없을 때 발사 커맨드 반환, 불가능하면 null
    // =============================================
    private static String shootCmd(int[] me, int[] target, String fireType) {
        // 같은 행 (좌우 사격)
        if (me[0] == target[0]) {
            int diff = target[1] - me[1];
            if (Math.abs(diff) >= 1 && Math.abs(diff) <= SHOOT_RANGE) {
                int step = diff > 0 ? 1 : -1;
                for (int c = me[1] + step; c != target[1]; c += step)
                    if (!isBulletPass(mapData[me[0]][c])) return null;
                return (diff > 0 ? "R " : "L ") + fireType;
            }
        }
        // 같은 열 (상하 사격)
        if (me[1] == target[1]) {
            int diff = target[0] - me[0];
            if (Math.abs(diff) >= 1 && Math.abs(diff) <= SHOOT_RANGE) {
                int step = diff > 0 ? 1 : -1;
                for (int r = me[0] + step; r != target[0]; r += step)
                    if (!isBulletPass(mapData[r][me[1]])) return null;
                return (diff > 0 ? "D " : "U ") + fireType;
            }
        }
        return null;
    }

    // 특정 위치 (r, c)에서 target을 일반 포탄으로 사격 가능한지 확인
    private static boolean canShootFrom(int r, int c, int[] target) {
        return shootCmd(new int[]{r, c}, target, "F") != null;
    }


    // =============================================
    // 시저 암호 자동 해독
    // 26가지 시프트를 모두 시도하여 영어 빈도 점수가 가장 높은 결과 반환
    // =============================================
    private static String decodeCaesar(String code) {
        int    bestShift = 0;
        double bestScore = Double.NEGATIVE_INFINITY;
        String bestPlain = code;

        for (int shift = 0; shift < 26; shift++) {
            StringBuilder sb = new StringBuilder();
            for (char ch : code.toCharArray()) {
                if ('A' <= ch && ch <= 'Z') sb.append((char)('A' + (ch - 'A' - shift + 26) % 26));
                else sb.append(ch);
            }
            String plain = sb.toString();
            double score = englishScore(plain);
            if (score > bestScore) {
                bestScore = score;
                bestShift = shift;
                bestPlain = plain;
            }
        }
        System.out.printf("[DECODE] shift=%d, score=%.1f → %s\n", bestShift, bestScore, bestPlain);
        return bestPlain;
    }

    // 영어 문장 유사도 점수 계산
    private static double englishScore(String s) {
        double score = 0;
        // 영어 고빈도 문자(E·T·A·O·I·N·S·H·R·D·L·U)에 점수 부여
        for (char ch : s.toCharArray())
            if ("ETAOINSHRDLU".indexOf(ch) >= 0) score += 1.0;
        // 공통 영어 단어·음절 등장 시 보너스
        for (String kw : new String[]{"THE", "AND", "ING", "ION", "YOU", "NOT", "ARE", "FOR", "WITH"})
            if (s.contains(kw)) score += 8.0;
        // 모음 비율이 자연스러운 영어 범위(15~60%)인지 검사
        int vowels = 0;
        for (char ch : s.toCharArray()) if ("AEIOU".indexOf(ch) >= 0) vowels++;
        double ratio = s.isEmpty() ? 0 : (double) vowels / s.length();
        if (ratio < 0.15 || ratio > 0.60) score -= 10.0;
        return score;
    }


    // =============================================
    // 지형 판별 헬퍼
    // =============================================
    private static boolean isMovable(String cell)    { return "G".equals(cell) || "S".equals(cell); }
    private static boolean isSand(String cell)       { return "S".equals(cell); }
    private static boolean isTree(String cell)       { return "T".equals(cell); }
    private static boolean isSupply(String cell)     { return "F".equals(cell); }
    // 포탄은 G·S·W를 통과, 나무·바위·적 유닛 등에서 막힘
    private static boolean isBulletPass(String cell) {
        return "G".equals(cell) || "S".equals(cell) || "W".equals(cell);
    }


    // =============================================
    // 위치·맵 유틸리티
    // =============================================
    private static boolean inBound(int r, int c) {
        return r >= 0 && r < mapData.length && c >= 0 && c < mapData[0].length;
    }

    // 맵에서 특정 심볼의 위치를 찾아 [row, col] 반환 (없으면 null)
    private static int[] findPos(String symbol) {
        for (int r = 0; r < mapData.length; r++)
            for (int c = 0; c < mapData[0].length; c++)
                if (symbol.equals(mapData[r][c])) return new int[]{r, c};
        return null;
    }

    // 특정 위치의 상하좌우 중 보급소(F)가 인접해 있는지 확인
    private static boolean isNextToSupply(int[] pos) {
        for (int d = 0; d < 4; d++) {
            int nr = pos[0] + dr[d], nc = pos[1] + dc[d];
            if (inBound(nr, nc) && isSupply(mapData[nr][nc])) return true;
        }
        return false;
    }

    // 맵 전체에 보급소(F)가 존재하는지 확인
    private static boolean hasSupplyOnMap() {
        for (String[] row : mapData)
            for (String cell : row)
                if (isSupply(cell)) return true;
        return false;
    }

    // 문자열을 정수로 변환 (실패 시 fallback 반환)
    private static int toInt(String s, int fallback) {
        try { return Integer.parseInt(s); } catch (Exception e) { return fallback; }
    }


    // =============================================
    // 입력 데이터를 파싱하여 변수에 저장
    // =============================================
    static void parseData(String gameData) {
        String[] rows = gameData.split("\n");
        int idx = 0;

        // 첫 번째 행: 맵 크기 및 각 항목 수
        String[] header  = rows[idx++].trim().split(" ");
        int mapH    = Integer.parseInt(header[0]);  // 맵의 세로 크기
        int mapW    = Integer.parseInt(header[1]);  // 맵의 가로 크기
        int allyCnt = Integer.parseInt(header[2]);  // 아군의 수
        int enemyCnt= Integer.parseInt(header[3]);  // 적군의 수
        int codeCnt = Integer.parseInt(header[4]);  // 암호문의 수

        // 기존의 맵 정보를 초기화하고 다시 읽어오기
        mapData = new String[mapH][mapW];
        for (int i = 0; i < mapH; i++)
            mapData[i] = rows[idx++].trim().split(" ");

        // 기존의 아군 정보를 초기화하고 다시 읽어오기
        allies.clear();
        for (int i = 0; i < allyCnt; i++) {
            String[] tokens = rows[idx++].trim().split(" ");
            allies.put(tokens[0], Arrays.copyOfRange(tokens, 1, tokens.length));
        }

        // 기존의 적군 정보를 초기화하고 다시 읽어오기
        enemies.clear();
        for (int i = 0; i < enemyCnt; i++) {
            String[] tokens = rows[idx++].trim().split(" ");
            enemies.put(tokens[0], Arrays.copyOfRange(tokens, 1, tokens.length));
        }

        // 기존의 암호문 정보를 초기화하고 다시 읽어오기
        codes = new String[codeCnt];
        for (int i = 0; i < codeCnt && idx < rows.length; i++)
            codes[i] = rows[idx++].trim();
    }
}
