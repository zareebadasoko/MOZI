import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import { defineConfig, globalIgnores } from 'eslint/config'

export default defineConfig([
  globalIgnores(['dist', '.vite']),
  {
    files: ['**/*.{js,jsx}'],
    extends: [
      js.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      globals: globals.browser,
      parserOptions: { ecmaFeatures: { jsx: true } },
    },
    rules: {
      // React 19 의 strict 룰. useEffect 안에서 setLoading/setError/setData 같은 표준
      // 데이터 fetch 패턴을 잡아내는데, 본 프로젝트의 모든 페이지가 이 패턴을 사용한다.
      // cascading render 영향은 한 commit 으로 끝나는 수준이라 성능 이슈 없음.
      'react-hooks/set-state-in-effect': 'off',
      // Context + 짝 hook(useAuth/useToast) 동시 export 는 표준 패턴.
      // Fast Refresh 가 약간 무거워질 뿐 기능 문제 없음 — Phase 6 정리에서는 단순성 유지.
      'react-refresh/only-export-components': 'off',
    },
  },
])
