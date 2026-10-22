#!/usr/bin/env python3
"""
Kotlin Token Validator - Checks for hardcoded values in Compose files
"""

import re
import os
import sys
from pathlib import Path

# Patterns to flag as hardcoded values
HARDCODED_PATTERNS = [
    # Hardcoded dp values
    (r'\b\d+(\.\d+)?\.dp\b', 'hardcoded dp value'),
    # Hardcoded sp values
    (r'\b\d+(\.\d+)?\.sp\b', 'hardcoded sp value'),
    # Direct MaterialTheme.colorScheme usage in components
    (r'MaterialTheme\.colorScheme\.', 'direct MaterialTheme color usage'),
    # Hardcoded colors
    (r'Color\(0x[0-9A-Fa-f]{8}\)', 'hardcoded Color hex'),
    (r'Color\.((Black|White|Transparent))', 'direct color reference'),
    # Hardcoded spacing
    (r'padding\s*\(\s*\d+', 'hardcoded padding'),
    # Hardcoded sizes
    (r'(width|height|size)\s*\(\s*\d+', 'hardcoded size'),
    # Hardcoded shapes
    (r'RoundedCornerShape\(\s*\d+', 'hardcoded radius'),
    (r'CircularProgressIndicator\([^)]*strokeWidth\s*=\s*\d+', 'hardcoded progress stroke'),
]

def check_file(filepath):
    """Check a Kotlin file for hardcoded values"""
    issues = []
    
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
            lines = content.split('\n')
    except Exception as e:
        print(f"Error reading {filepath}: {e}")
        return issues
    
    for line_num, line in enumerate(lines, 1):
        for pattern, desc in HARDCODED_PATTERNS:
            matches = re.finditer(pattern, line)
            for match in matches:
                # Skip comments
                if line.strip().startswith('//') or line.strip().startswith('*'):
                    continue
                # Skip in strings
                if '"' in line or "'" in line:
                    continue
                issues.append({
                    'file': filepath,
                    'line': line_num,
                    'pattern': desc,
                    'match': match.group(),
                    'code': line.strip()[:100]
                })
    
    return issues

def check_directory(directory):
    """Check all Kotlin files in a directory"""
    all_issues = []
    
    for root, dirs, files in os.walk(directory):
        # Skip build and .git directories
        dirs[:] = [d for d in dirs if d not in ['build', '.git', '__pycache__']]
        
        for file in files:
            if file.endswith('.kt'):
                filepath = os.path.join(root, file)
                issues = check_file(filepath)
                all_issues.extend(issues)
    
    return all_issues

def main():
    if len(sys.argv) < 2:
        print("Usage: python validate-kotlin-tokens.py <directory>")
        sys.exit(1)
    
    directory = sys.argv[1]
    
    if not os.path.isdir(directory):
        print(f"Error: {directory} is not a directory")
        sys.exit(1)
    
    print(f"Checking Kotlin files in {directory}...")
    issues = check_directory(directory)
    
    if issues:
        print(f"\nFound {len(issues)} potential issues:\n")
        for issue in issues[:50]:  # Limit output
            print(f"  {issue['file']}:{issue['line']}")
            print(f"    [{issue['pattern']}] {issue['match']}")
            print(f"    {issue['code']}")
            print()
        
        if len(issues) > 50:
            print(f"... and {len(issues) - 50} more issues")
        
        print(f"\nTotal: {len(issues)} issues found")
        print("Consider using ComponentTokens.* instead of hardcoded values")
    else:
        print("✅ No hardcoded values found! Token usage looks good.")

if __name__ == '__main__':
    main()