# Color Usage Guide

This document outlines the standardized color scheme used in `messages.yml` to ensure a consistent and clean user experience.

All colors are defined using `& #RRGGBB` hex codes.

## Color Palette

| Color Name        | Hex Code    | Usage                                                              |
| ----------------- | ----------- | ------------------------------------------------------------------ |
| **Primary (Gold)**  | `& #FFD700`  | Titles, important keywords, prefixes, and currency (Coins).        |
| **Secondary (Cyan)**| `& #55FFFF`  | Player actions, commands, and highlights.                          |
| **Text (White)**    | `& #FFFFFF`  | Standard text and main content.                                    |
| **Muted Text (Gray)**| `& #AAAAAA`  | Descriptions, lore, and less important text.                       |
| **Success (Green)** | `& #55FF55`  | Success messages, completion notifications, and positive feedback. |
| **Error (Red)**     | `& #FF5555`  | Error messages, cancellation confirmations, and negative feedback.   |
| **Warning (Yellow)**| `& #FFFF55`  | Warnings, tips, and progress indicators.                           |
| **Currency (Pink)** | `& #FF55FF`  | Used for SkillTokens currency.                                     |
| **Locked (Dark Gray)**| `& #777777`  | Indicates locked or disabled features and menu items.              |

## Best Practices

*   **Consistency is Key:** Always use the defined colors for their intended purpose.
*   **Avoid Old Codes:** Do not use old `&` color codes (e.g., `&a`, `&c`). Always use the hex format.
*   **Readability:** Ensure that the color combinations provide good readability. Avoid using light colors on light backgrounds or dark colors on dark backgrounds (though this is less of an issue in chat).

## Example

Here is an example of a well-formatted message using the new color scheme:

```yaml
quest:
  started: "& #55FF55You've started the tutorial quests! Follow each step to learn the basics."
  already-started: "& #FF5555You've already started the tutorial!"
```
