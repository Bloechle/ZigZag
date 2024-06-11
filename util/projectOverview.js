const fs = require('fs');
const path = require('path');

// Paths relative to the project root to ignore
const ignoredPaths = [
    'sugarcube/overview',
];

// Non-text file extensions to ignore
const nonTextFileExtensions = ['.png', '.jpg', '.jpeg', '.gif', '.bmp', '.tiff', '.ico', '.svg', '.webp', '.map'];

const outputFilePath = path.join(__dirname, 'project_overview.txt');
const rootDir = path.resolve(__dirname, '..', 'src'); // Starts from the script's parent directory

// Check if a given file path should be ignored based on project structure
function shouldIgnorePath(filePath, rootDir) {
    const relativePath = path.relative(rootDir, filePath).replace(/\\/g, '/'); // Normalize slashes
    return ignoredPaths.some(ignoredPath => relativePath.startsWith(ignoredPath)) ||
        nonTextFileExtensions.some(ext => filePath.endsWith(ext));
}

// Generate directory structure text recursively
function generateDirectoryStructureText(dir, rootDir, depth = 0) {
    const prefix = ' '.repeat(depth * 4);
    const files = fs.readdirSync(dir);
    let structure = '';

    files.forEach((file, index) => {
        const filePath = path.join(dir, file);

        if (shouldIgnorePath(filePath, rootDir)) {
            return; // Ignore the file or directory
        }

        structure += `${prefix}${file}\n`;

        if (fs.statSync(filePath).isDirectory()) {
            structure += generateDirectoryStructureText(filePath, rootDir, depth + 1);
        }
    });

    return structure;
}

// Merge JavaScript files recursively
function mergeJavaFiles(dir, rootDir) {
    const files = fs.readdirSync(dir);

    files.forEach((file) => {
        const filePath = path.join(dir, file);

        if (shouldIgnorePath(filePath, rootDir)) {
            return; // Ignore the file or directory
        }

        if (fs.statSync(filePath).isDirectory()) {
            mergeJavaFiles(filePath, rootDir);
        } else if (filePath.endsWith('.java')) {
            const relativePath = path.relative(rootDir, filePath).replace(/\\/g, '/');
            const fileContent = fs.readFileSync(filePath, 'utf8');
            const contentToAdd = `\n\n/* Path: ${relativePath} */\n\n${fileContent}\n`;
            fs.appendFileSync(outputFilePath, contentToAdd);
        }
    });
}

// Delete the output file if it already exists
if (fs.existsSync(outputFilePath)) {
    fs.unlinkSync(outputFilePath);
}

// Add initial comment to the output file
const initialComment = `/*
 * Project Overview:
 * 
 * This document provides an overview of the project, detailing its directory structure and key file contents. 
 * It aims to facilitate AI and GPT comprehension of the project's architecture and functionality.
 * 
 * Project Description:
 * - The project is an image binarization tool written in Java 8 (very important).
 *
 * Instructions for AI:
 * - Upon processing this overview, AI should respond with "OK, I'm ready :-)", without further prompt, and be prepared to answer detailed questions.
 * - When generating Java code (upon a subsequent prompt), avoid using Java comments and use English for logs :-)
 * - Files are separated by '/* Path: <filepath> */' comments to indicate their locations.
 */
`;
fs.writeFileSync(outputFilePath, initialComment, 'utf8');

// Add directory structure as a comment
const timestamp = new Date().toISOString();
const directoryStructureText = generateDirectoryStructureText(rootDir, rootDir);
const directoryStructureComment = `/*
Directory Structure of the Project (as of ${timestamp}):\n\n${directoryStructureText}\n*/\n`;
fs.appendFileSync(outputFilePath, directoryStructureComment);

mergeJavaFiles(rootDir, rootDir);

console.log('Project overview generated successfully.');
