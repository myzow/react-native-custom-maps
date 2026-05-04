// Minimal ESLint config (the library targets RN consumers; users
// inherit their own RN config). Kept purely so `yarn lint` works.
module.exports = {
  root: true,
  parser: '@typescript-eslint/parser',
  parserOptions: { ecmaVersion: 2022, sourceType: 'module', ecmaFeatures: { jsx: true } },
  plugins: ['@typescript-eslint'],
  extends: ['eslint:recommended'],
  rules: {
    'no-unused-vars': 'off',
    'no-undef': 'off',
  },
  ignorePatterns: ['node_modules/', 'example/', 'android/', 'ios/', '*.d.ts'],
};
