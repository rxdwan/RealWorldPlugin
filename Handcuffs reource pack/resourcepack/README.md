# RealWorld Resource Pack

This folder contains the custom textures and models needed for the RealWorld plugin (e.g., the Citizen's Arrest Cuffs).

## How to Install and Host
1. Select all the contents inside this `resourcepack/` folder (the `assets/` folder and `pack.mcmeta` file).
2. Compress them into a ZIP file (e.g., `realworld-pack.zip`).
3. Upload the `.zip` file to a direct download file host (like Dropbox, GitHub Releases, or a dedicated resource pack host).
   - *Note: Ensure the link is a **direct** download link (ends in `.zip` or uses `dl=1`).*
4. Open your Minecraft server's `server.properties` file.
5. Set `resource-pack=YOUR_DIRECT_DOWNLOAD_LINK`
6. (Optional but recommended) Generate the SHA-1 hash of your zip file and set `resource-pack-sha1=YOUR_HASH`.
7. Restart your server. Players will be prompted to download the pack when they join!
