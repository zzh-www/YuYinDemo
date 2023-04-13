echo "=================before======================"
ls -al "app/src/main/assets"
echo "============================================="
filename="final_en.zip"
fileid="1XSrU-vki1kDHxHIzXSAkL5JOjohiOQo6"
if [ ! -f "app/src/main/assets/${filename}" ];then
  wget --load-cookies /tmp/cookies.txt "https://drive.google.com/uc?export=download&confirm=$(wget --quiet --save-cookies /tmp/cookies.txt --keep-session-cookies --no-check-certificate 'https://drive.google.com/uc?export=download&id=${fileid}' -O- | sed -rn 's/.confirm=([0-9A-Za-z_]+)./\1\n/p')&id=${fileid}" -O ${filename} && rm -rf /tmp/cookies.txt
  mv "${filename}" "app/src/main/assets/${filename}"
fi
filename="final_zh.zip"
fileid="1QXC4DMmmFnmSvUtoeBaqHzbIkYPNEj33"
if [ ! -f "app/src/main/assets/${filename}" ];then
  wget --load-cookies /tmp/cookies.txt "https://drive.google.com/uc?export=download&confirm=$(wget --quiet --save-cookies /tmp/cookies.txt --keep-session-cookies --no-check-certificate 'https://drive.google.com/uc?export=download&id=${fileid}' -O- | sed -rn 's/.confirm=([0-9A-Za-z_]+)./\1\n/p')&id=${fileid}" -O ${filename} && rm -rf /tmp/cookies.txt
  mv "${filename}" "app/src/main/assets/${filename}"
fi
echo "=================done======================"
ls -al "app/src/main/assets"
echo "============================================="