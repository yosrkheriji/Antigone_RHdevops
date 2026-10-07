/**
 * Les imports CSS sont resolus par le bundler de l'application hote (Vite), pas
 * par TypeScript. Cette declaration evite une erreur de compilation sur un import
 * dont l'effet est purement runtime.
 */
declare module '*.css';
