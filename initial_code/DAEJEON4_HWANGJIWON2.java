package com.example.main;

import com.example.libraries.Bridge;

import java.util.*;

public class DAEJEON4_HWANGJIWON2 {
    private static String NICKNAME = "DAEJEON4_HWANGJIWON2";
    private static String[][] mapData; // 맵 정보
    private static Map<String, String[]> allies = new HashMap<>(); // 아군 정보
    private static Map<String, String[]> enemies = new HashMap<>(); // 적군 정보
    private static String[] codes; // 암호문 정보

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
                    System.out.printf("A (내 탱크) - 체력: %s, 방향: %s, 보유한 일반 포탄: %s개, 보유한 대전차 포탄: %s개\n", value[0],
                            value[1], value[2], value[3]);
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
                    System.out.printf("H (적군 포탑) - 체력: %s\n", value[0]);
                } else {
                    System.out.printf("%s (적군 탱크) - 체력: %s\n", key, value[0]);
                }
            }

            System.out.printf("\n[암호문 정보] (암호문 수: %d)\n", codes.length);
            for (int i = 0; i < codes.length; i++) {
                System.out.printf("%s\n", codes[i]);
            }

            int[] myPosition = { -1, -1 };
            String output = "S";

            for (int i = 0; i < mapData.length; i++) {
                for (int j = 0; j < mapData[i].length; j++) {
                    if (mapData[i][j].equals("A")) {
                        myPosition[0] = i;
                        myPosition[1] = j;
                        break;
                    }
                }
                if (myPosition[0] > 0)
                    break;
            }

            result = new ArrayDeque<>();
            visited = new boolean[mapData.length][mapData.length];
            bfsX(myPosition[0], myPosition[1]);

            output = result.poll();

            gameData = bridge.submit(output);
        }

        // 반복문을 빠져나왔을 때 배틀싸피 메인 프로그램과의 연결을 완전히 해제하기 위해 close 함수 호출
        bridge.close();
    }

    static Queue<String> result;
    static boolean[][] visited;
    static int[] dr = { -1, 1, 0, 0 };
    static int[] dc = { 0, 0, -1, 1 };
    static String[] move = { "U A", "D A", "L A", "R A" };
    static String[] fire = { "U F", "D F", "L F", "R F" };

    static char[] Alphabet = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R',
            'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' };

    static void bfsX(int i, int j) {
        System.out.println("****************************************");
        Queue<Node2> queue = new LinkedList<>();
        ArrayList<String> path = new ArrayList<>();

        Node2 node = new Node2(i, j, path);

        queue.add(node);

        while (!queue.isEmpty()) {
            Node2 tmp = queue.poll();

            int r = tmp.r;
            int c = tmp.c;
            path = tmp.path;

            if (mapData[r][c].equals("E3")) {
                for (int n = 0; n < path.size(); n++) {
                    result.add(path.get(n));
                }
                return;
            } else if (mapData[r][c].equals("E2")) {
                for (int n = 0; n < path.size(); n++) {
                    result.add(path.get(n));
                }
                return;
            } else if (mapData[r][c].equals("E1")) {
                for (int n = 0; n < path.size(); n++) {
                    result.add(path.get(n));
                }
                return;
            }

            for (int d = 0; d < 4; d++) {
                int nr = r + dr[d];
                int nc = c + dc[d];

                if (nr >= 0 && nr < mapData.length && nc >= 0 && nc < mapData.length && !visited[nr][nc]
                        && mapData[nr][nc].equals("G")) {
                    path.add(move[d]);
                    ArrayList<String> temp = new ArrayList<>();
                    for (int n = 0; n < path.size(); n++) {
                        temp.add(path.get(n));
                    }
                    queue.add(new Node2(nr, nc, temp));
                    path.remove(path.size() - 1);
                    visited[nr][nc] = true;
                } else if (nr >= 0 && nr < mapData.length && nc >= 0 && nc < mapData.length && !visited[nr][nc]
                        && mapData[nr][nc].equals("X")) {
                    path.add(fire[d]);
                    ArrayList<String> temp = new ArrayList<>();
                    for (int n = 0; n < path.size(); n++) {
                        temp.add(path.get(n));
                    }
                    queue.add(new Node2(nr, nc, temp));
                    path.remove(path.size() - 1);
                    visited[nr][nc] = true;
                } else if (nr >= 0 && nr < mapData.length && nc >= 0 && nc < mapData.length && !visited[nr][nc]
                        && mapData[nr][nc].equals("E1")) {
                    path.add(fire[d] + " S");
                    ArrayList<String> temp = new ArrayList<>();
                    for (int n = 0; n < path.size(); n++) {
                        temp.add(path.get(n));
                    }
                    queue.add(new Node2(nr, nc, temp));
                    path.remove(path.size() - 1);
                    visited[nr][nc] = true;
                } else if (nr >= 0 && nr < mapData.length && nc >= 0 && nc < mapData.length && !visited[nr][nc]
                        && mapData[nr][nc].equals("E2")) {
                    path.add(fire[d] + " S");
                    ArrayList<String> temp = new ArrayList<>();
                    for (int n = 0; n < path.size(); n++) {
                        temp.add(path.get(n));
                    }
                    queue.add(new Node2(nr, nc, temp));
                    path.remove(path.size() - 1);
                    visited[nr][nc] = true;
                } else if (nr >= 0 && nr < mapData.length && nc >= 0 && nc < mapData.length && !visited[nr][nc]
                        && mapData[nr][nc].equals("E3")) {
                    path.add(fire[d] + " S");
                    ArrayList<String> temp = new ArrayList<>();
                    for (int n = 0; n < path.size(); n++) {
                        temp.add(path.get(n));
                    }
                    queue.add(new Node2(nr, nc, temp));
                    path.remove(path.size() - 1);
                    visited[nr][nc] = true;
                }
            }
        }
    }

    static void bfsF(int i, int j) {
        Queue<Node2> queue = new LinkedList<>();
        ArrayList<String> path = new ArrayList<>();

        Node2 node = new Node2(i, j, path);

        queue.add(node);

        while (!queue.isEmpty()) {
            Node2 tmp = queue.poll();

            int r = tmp.r;
            int c = tmp.c;
            path = tmp.path;

            if (mapData[r][c].equals("F")) {
                for (int n = 0; n < path.size(); n++) {
                    result.add(path.get(n));
                }
                return;
            }

            for (int d = 0; d < 4; d++) {
                int nr = r + dr[d];
                int nc = c + dc[d];

                if (nr >= 0 && nr < mapData.length && nc >= 0 && nc < mapData.length && !visited[nr][nc]
                        && mapData[nr][nc].equals("G")) {
                    path.add(move[d]);
                    ArrayList<String> temp = new ArrayList<>();
                    for (int n = 0; n < path.size(); n++) {
                        temp.add(path.get(n));
                    }
                    queue.add(new Node2(nr, nc, temp));
                    path.remove(path.size() - 1);
                    visited[nr][nc] = true;
                } else if (nr >= 0 && nr < mapData.length && nc >= 0 && nc < mapData.length && !visited[nr][nc]
                        && mapData[nr][nc].equals("F")) {
                    ArrayList<String> temp = new ArrayList<>();
                    for (int n = 0; n < path.size(); n++) {
                        temp.add(path.get(n));
                    }
                    queue.add(new Node2(nr, nc, temp));
                    path.remove(path.size() - 1);
                    visited[nr][nc] = true;
                }
            }
        }
    }

    // 입력 데이터를 파싱하여 변수에 저장
    static void parseData(String gameData) {
        // 입력 데이터를 행으로 나누기
        String[] rows = gameData.split("\n");
        int rowIndex = 0;

        // 첫 번째 행 데이터 읽기
        String[] header = rows[rowIndex].split(" ");
        int mapHeight = Integer.parseInt(header[0]); // 맵의 세로 크기
        int mapWidth = Integer.parseInt(header[1]); // 맵의 가로 크기
        int numOfAllies = Integer.parseInt(header[2]); // 아군의 수
        int numOfEnemies = Integer.parseInt(header[3]); // 적군의 수
        int numOfCodes = Integer.parseInt(header[4]); // 암호문의 수
        rowIndex++;

        // 기존의 맵 정보를 초기화하고 다시 읽어오기
        mapData = new String[mapHeight][mapWidth];
        for (int i = 0; i < mapHeight; i++) {
            mapData[i] = rows[rowIndex + i].split(" ");
        }
        rowIndex += mapHeight;

        // 기존의 아군 정보를 초기화하고 다시 읽어오기
        allies.clear();
        for (int i = 0; i < numOfAllies; i++) {
            String[] allyData = rows[rowIndex + i].split(" ");
            String allyName = allyData[0];
            String[] allyInfo = Arrays.copyOfRange(allyData, 1, allyData.length);
            allies.put(allyName, allyInfo);
        }
        rowIndex += numOfAllies;

        // 기존의 적군 정보를 초기화하고 다시 읽어오기
        enemies.clear();
        for (int i = 0; i < numOfEnemies; i++) {
            String[] enemyData = rows[rowIndex + i].split(" ");
            String enemyName = enemyData[0];
            String[] enemyInfo = Arrays.copyOfRange(enemyData, 1, enemyData.length);
            enemies.put(enemyName, enemyInfo);
        }
        rowIndex += numOfEnemies;

        // 기존의 암호문 정보를 초기화하고 다시 읽어오기
        codes = new String[numOfCodes];
        for (int i = 0; i < numOfCodes; i++) {
            codes[i] = rows[rowIndex + i];
        }
    }
}

class Node2 {
    int r;
    int c;
    ArrayList<String> path;

    public Node2(int r, int c, ArrayList<String> path) {
        this.r = r;
        this.c = c;
        this.path = path;
    }

    public Node2() {
    }
}