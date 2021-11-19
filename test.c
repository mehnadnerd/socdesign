#include <inttypes.h>
#include <stdio.h>

#define M (8)
#define N (8)
#define K (8)

#define intype int16_t
#define outtype int32_t

outtype C[M][N];

intype a(int i, int j, int k) {
    return (i * K + k) * sizeof(intype);
}

intype b(int i, int j, int k) {
    return (k * N + j) * sizeof(intype);
}

int main() {
    for (int i = 0; i < M; ++i) {
        for (int j = 0; j < N; ++j) {
            C[i][j] = 0;
        }
    }

    for (int i = 0; i < M; ++i) {
        for (int j = 0; j < N; ++j) {
            for (int k = 0; k < K; ++k) {
                intype A = a(i, j, k);
                intype B = b(i, j, k);
                C[i][j] += (outtype) A * (outtype) B;
            }
        }
    }
    for (int i = 0; i < M; ++i) {
        for (int j = 0; j < N; ++j) {
            printf("%04x %08x\n", (i * N + j) * sizeof(outtype), C[i][j]);
        }
    }

    return 0;
}