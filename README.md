# RealWorld

RealWorld is a feature-rich roleplay crime and jail system for **Paper 1.21+** Minecraft servers. It allows administrators to charge players with specific crimes, adding a bounty to their heads. Citizens can then enforce the law using Citizen's Arrest Cuffs to send wanted criminals directly to jail and claim the bounty.

## Disclaimer
This project is made with the help of [Antigravity IDE](https://antigravity.google/). This is a vibecoded project!
> *Not sponsored btw.*

## Features

*   **Dynamic Crime System:** Administrators can charge players with predefined, configurable crimes.
*   **Bounty System:** Each crime adds a configurable bounty to the player.
*   **Citizen's Arrests:** Players can obtain Handcuffs to arrest wanted criminals, claiming their bounty as a reward.
*   **Jail System:** Jailed players have their inventories securely confiscated and are restricted from using unapproved commands until they are released or pay bail.
*   **Vault Integration:** Fully integrated with Vault for economy support (paying bail and claiming bounties).
*   **Crime Logging:** Detailed logs of all charges, arrests, pardons, and bail payments, accessible via command.

## Commands

### Player Commands
*   `/realworld info` - View plugin version and author information.
*   `/wantedlist` - Display a clean, formatted table of all wanted criminals and their bounties.
*   `/inmates` - Display a formatted table of all players currently serving time in jail, their crimes, and their bail amount.
*   `/cuffs` - Receive a pair of Citizen's Arrest Cuffs.
*   `/bail` - Pay your bail to be instantly released from jail and have your items returned.

### Admin Commands (Requires `realworld.admin` permission)
*   `/realworld reload` - Reload the configuration files without restarting the server.
*   `/chargecrime <player> <crime>` - Charge a player with a specific crime from the config.
*   `/pardon <player> [crime]` - Pardon a player from a specific crime, or completely wipe their record if no crime is specified.
*   `/crimelog [limit]` - View the server's crime history log (default shows the last 10 entries).

## Configuration

The plugin is highly configurable through `config.yml`. You can define custom crimes, adjust the bounty increment, set the jail and exit locations, set the bail amount, and whitelist specific commands for jailed players. 

## Requirements
*   Vault
*   A compatible economy plugin like EssentialsX

## Screenshots
<center>
    <img src="handcuffs in-game screenshot.png" width="400" alt="image of handcuffs as a dropped item in minecraft">
</center>

## Building from Source

Requires Java 17+ and Maven (or `mvnd` for faster builds).

```bash
git clone https://github.com/rxdwan/RealWorldPlugin.git
cd RealWorldPlugin
mvnd clean package
```

## License
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
