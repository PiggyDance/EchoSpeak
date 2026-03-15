/* Minimal os_support.h stub for building RNNoise without full Opus dependency. */
#ifndef OS_SUPPORT_H
#define OS_SUPPORT_H

#include <string.h>  /* memset */

/* 
 * common.h already defines OPUS_INLINE as 'inline'.
 * We only define it if not already defined to avoid "macro redefined" warning.
 */
#ifndef OPUS_INLINE
#  ifdef _MSC_VER
#    define OPUS_INLINE __inline
#  else
#    define OPUS_INLINE static inline
#  endif
#endif

/* 
 * opus_types.h defines opus_int8, opus_int32 etc. as macros.
 * We don't need typedefs here as they would conflict.
 */

/* Zero-fill helper used in vec_neon.h */
#ifndef OPUS_CLEAR
#  define OPUS_CLEAR(dst, n) (memset((dst), 0, (n) * sizeof(*(dst))))
#endif

#endif /* OS_SUPPORT_H */
