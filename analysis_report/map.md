# 🧠 맵 생성 및 변화 정책 분석

## 1. 📊 결론 (핵심 요약)

| 구간        | 맵 특성                              | 변경 여부 |
| ----------- | ------------------------------------ | --------- |
| Stage 1 ~ 4 | **고정 맵 (Seed 고정 or 완전 고정)** | ❌ 없음   |
| Stage 5     | **고정 패턴 + 일부 랜덤 변형**       | ⚠️ 약간   |
| Stage 6     | **턴마다 동적 변경 맵**              | ⭕ 있음   |

---

# 2. 🔍 로그 기반 근거 분석

## 2-1. Stage 1 ~ 4

로그 특징:

```text
동일 입력 반복 시
맵 구조 완전히 동일
```

- 여러 실행에서도 동일한 패턴 반복
- 위치, 장애물, 목표 동일

👉 결론

```text
완전 고정 맵 또는 seed 고정
```

---

## 2-2. Stage 5

로그 특징:

- 동일 실행 내에서는 맵 유지
- 하지만 실행마다 아래 패턴이 바뀜

### 패턴 A

- 하단 평지 → 우측 이동 → 상단 접근

### 패턴 B

- 상단 나무 밀집 → 파괴 후 진입

👉 결론

```text
템플릿 기반 랜덤 생성
```

- 구조는 동일
- 일부 블록만 변경

---

## 2-3. Stage 6

로그 특징 (가장 중요)

```text
같은 턴인데 맵이 바뀜
```

실제 로그:

- 동일 턴 번호에서
- 같은 좌표의 타일이 변경됨
- 적 위치 변동
- 일부 지형 재배치

👉 결론

```text
턴 기반 동적 맵 업데이트
```

---

# 3. 🧩 Stage별 맵 구조 (완전 버전)

---

# 3-1. Stage 1 (고정)

```text
G G G G G G G G
G G G G G G G G
R R R R R R G G
R R R G X R G G
R R G G R R G G
R G G R R R G G
M G R R R R G G
R R R R R R G G
```

---

# 3-2. Stage 2 (고정)

```text
G G G G G G W G
G G G G G G W G
R R R R R R G G
R R R G X R G G
R R G G R R G G
R G G R R R G G
M G R R R R G G
R R R R R R G G
```

---

# 3-3. Stage 3 (고정)

```text
G G G G G G G G
G G G G G G G G
R R R R R R G G
R R R G X R G G
R R G G R R G G
R G W W W R G G
M G G G G G G G
R R R R R R G G
```

---

# 3-4. Stage 4 (고정)

```text
G G G G G G G X
G G G G G G G G
R R R R R R G G
R R R G G R G G
R R G G R R G G
R G W W W R G G
M G G G G G G G
R R R R R R G G
```

---

# 3-5. Stage 5 (패턴 기반)

## 패턴 A

```text
G G G G G R G W W X
G R G R G R G W W W
G W R G G R G G G G
G W G G G G R G R G
G W W G G G G G R G
G G W G G T T T R G
G G G R T T R G T T
G W G R R R R G W W
M W G G G G G G G G
G W G G G G G G G G
```

---

## 패턴 B

```text
R G G G G G W G G X
G T T T T G G G R G
G W T T T G G G G G
G W W T T R R R R G
G W W W T G G R G G
G G W G G T T T G G
G G G R T T R G G G
G W G R R R R G W W
M W G G G G G G G G
G W G G G G G G G G
```

---

# 3-6. Stage 6 (동적 맵)

## 초기 형태 (턴 0 기준)

```text
G G G G F G R G G F G G G W W X
G G G G G G R G G G G G G W W W
R R R G R G R G R R G G G W W W
G G G G R G R G G R G G G G G G
G R R R T G R R G R G G G G G E1
G G G R T G R G G R G G R R G G
G R G R T G G G R R G G G R G E2
G R G R T T G R G G G W G R R R
R R G R T T G R G G G W G G G E3
G G G G T T G R G G G W G G G G
G G G G G G G R G G G W G G G G
G G G G G G G R G G G G G G G G
G G G G G G G G G G G G G G G G
G G G G G G G G G G G G G G G G
M G G G G G G G G G G G G G G G
G G G G G G G G G G G G G G G G
```

---

## Stage 6 변화 특징

### 1. 적 이동

```text
E1, E2, E3 위치 변화
```

---

### 2. 지형 변화

- 일부 `G ↔ W`
- 일부 `T 생성/삭제`

---

### 3. 보급소 유지

```text
F 위치는 고정
```

👉 매우 중요한 규칙

---

# 4. 🧠 Stage 6 핵심 구조

## 정적 요소

| 요소    | 특징 |
| ------- | ---- |
| F       | 고정 |
| X       | 고정 |
| 맵 크기 | 고정 |

---

## 동적 요소

| 요소     | 변화 |
| -------- | ---- |
| E1~E3    | 이동 |
| 일부 G/W | 변경 |
| 일부 T   | 생성 |

---

# 5. 🔥 중요한 설계 포인트

## 5-1. Stage 1~4

```text
완전 그래프 문제
```

---

## 5-2. Stage 5

```text
그래프 + 파괴 가능한 간선
```

---

## 5-3. Stage 6

```text
시간에 따라 변하는 그래프
(Time-dependent graph)
```

---

# 6. 💡 핵심 인사이트

## 1. Stage 6은 다른 게임이다

```text
정적 BFS ❌
동적 환경 대응 ⭕
```

---

## 2. 경로는 고정하면 안 된다

```text
매 턴 재계산 필요
```

---

## 3. 보급소는 "중간 목표"

```text
F → decode → 다시 목표
```

---

# 7. 🔚 최종 결론

이 게임의 맵 시스템은 다음과 같이 구성됩니다.

```text
Stage 1~4 : 고정 그래프
Stage 5    : 부분 랜덤 그래프
Stage 6    : 시간에 따라 변하는 그래프
```

---
