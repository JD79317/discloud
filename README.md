# Discloud

Discloud allows you to store or backup your files in the cloud using discord.
The user interface is made using java swing.

## How Does It work?

Selected files/folders are copied, encrypted and split into 8MB segments, they are then uploaded as attachments by a
discord bot into an arbitrary discord channel. This uploads them to discord's CDN which will retain the files for as
long as the messages exist.

Returned links for the files, directory structure and the encryption key are then stored in a tiny .discloud file which
can be used to download the files at a later date using the program.

## How to Upload Files

1. Create a [discord bot](https://discordpy.readthedocs.io/en/stable/discord.html) then invite the bot to any discord
   server.
2. Ensure the bot has permissions to send messages and attach files.
3. Make sure that discord has a text channel that the bot has permission to view.
3. Click the "upload files to cdn" button and select the files you'd like to upload.
4. Enter your discord bot token
5. Enter the name of the text channel you want the files to be uploaded into.
6. Select the location you'd like to save the .discloud file.
7. Wait for files to upload.
8. You're done!

## How to Download Files

1. Simply drag and drop the .discloud file or click the "download files from cdn" button.
2. select which files you would like to download, it will download them into the same directory as the discloud file.
3. Wait for the files to download.
4. You're Done!