/**
 * Metro config for the example app.
 *
 * The library is consumed via `link:..` in package.json, but Metro needs an
 * explicit watchFolder + module resolution shim so Babel/TS pick up the
 * library sources directly (so changes inside ../src hot-reload).
 */
const path = require('path');
const { getDefaultConfig, mergeConfig } = require('@react-native/metro-config');

const projectRoot = __dirname;
const libraryRoot = path.resolve(projectRoot, '..');

const config = {
  watchFolders: [libraryRoot],
  resolver: {
    nodeModulesPaths: [
      path.join(projectRoot, 'node_modules'),
      path.join(libraryRoot, 'node_modules'),
    ],
    extraNodeModules: new Proxy(
      {},
      {
        get: (_, name) => {
          if (name === 'react-native-custom-maps') return libraryRoot;
          return path.join(projectRoot, 'node_modules', name);
        },
      },
    ),
  },
};

module.exports = mergeConfig(getDefaultConfig(projectRoot), config);
