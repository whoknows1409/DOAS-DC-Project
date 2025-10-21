import { useTheme } from '../contexts/ThemeContext';

export const useThemeColors = () => {
  const { isDarkMode } = useTheme();

  return {
    // Background colors
    bgPrimary: isDarkMode ? '#010226' : '#ffffff',
    bgSecondary: isDarkMode ? '#011836' : '#f4fcfe',
    bgTertiary: isDarkMode ? '#012451' : '#e9f9fc',
    cardBg: isDarkMode ? '#011836' : '#ffffff',
    headerBg: isDarkMode ? '#010226' : '#023e8a',

    // Text colors
    textPrimary: isDarkMode ? '#effafd' : '#010226',
    textSecondary: isDarkMode ? '#caf0f8' : '#012451',
    textTertiary: isDarkMode ? '#90e0ef' : '#00486e',

    // Border colors
    borderColor: isDarkMode ? '#012451' : '#d2f3f9',

    // Accent colors
    accentPrimary: isDarkMode ? '#48cae4' : '#0077b6',
    accentSecondary: isDarkMode ? '#00b4d8' : '#00b4d8',
    accentTertiary: isDarkMode ? '#90e0ef' : '#48cae4',

    // Status colors
    successColor: isDarkMode ? '#0096c7' : '#0096c7',
    warningColor: isDarkMode ? '#48cae4' : '#00b4d8',
    errorColor: isDarkMode ? '#0077b6' : '#023e8a',
    infoColor: isDarkMode ? '#ade8f4' : '#90e0ef',

    // Gradients
    gradientPrimary: isDarkMode 
      ? 'linear-gradient(135deg, #0077b6 0%, #48cae4 100%)'
      : 'linear-gradient(135deg, #0077b6 0%, #00b4d8 100%)',
    gradientSecondary: 'linear-gradient(135deg, #023e8a 0%, #0077b6 100%)',
    gradientLogin: 'linear-gradient(135deg, #023e8a 0%, #0077b6 50%, #00b4d8 100%)',

    // Shadows
    shadowSm: isDarkMode 
      ? '0 1px 3px 0 rgba(0, 0, 0, 0.5), 0 1px 2px 0 rgba(0, 0, 0, 0.3)'
      : '0 1px 3px 0 rgba(3, 4, 94, 0.1), 0 1px 2px 0 rgba(3, 4, 94, 0.06)',
    shadowMd: isDarkMode
      ? '0 4px 6px -1px rgba(0, 0, 0, 0.5), 0 2px 4px -1px rgba(0, 0, 0, 0.3)'
      : '0 4px 6px -1px rgba(3, 4, 94, 0.1), 0 2px 4px -1px rgba(3, 4, 94, 0.06)',
    shadowLg: isDarkMode
      ? '0 10px 15px -3px rgba(0, 0, 0, 0.5), 0 4px 6px -2px rgba(0, 0, 0, 0.3)'
      : '0 10px 15px -3px rgba(3, 4, 94, 0.1), 0 4px 6px -2px rgba(3, 4, 94, 0.05)',
  };
};


