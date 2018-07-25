/****************
 * Encryption, decryption
 ***************/

let rkEncryptionKey;
let rkEncryptionIv;

/**
 * Initialize encryption key and Initial Vector
 */
function initEncryption() {
  rkEncryptionKey = CryptoJS.enc.Base64.parse($("#encryption_key").val());
  rkEncryptionIv = CryptoJS.enc.Base64.parse($("#encryption_iv").val());
}

/**
 * Stream name decode
 * @param  {String} stringToEncrypt Plain text string
 * @return {String}                 Encrypted character string
 */
function end(stringToEncrypt){
  let utf8Stringified = CryptoJS.enc.Utf8.parse(stringToEncrypt);
  let encrypted = CryptoJS.AES.encrypt(utf8Stringified, rkEncryptionKey,
    {
      mode: CryptoJS.mode.CBC,
      padding: CryptoJS.pad.Pkcs7,
      iv: rkEncryptionIv
    }
  );

  return encrypted.ciphertext.toString(CryptoJS.enc.Base64);
}

/**
 * Stream name decode
 * @param  {String} encryptToString Encrypted character string
 * @return {String}                 Plain text string
 */
function dec(encryptToString){
  if(encryptToString == ""){
    return "";
  }

  const crypted = CryptoJS.enc.Base64.parse(encryptToString);

  let decrypt = CryptoJS.AES.decrypt({ciphertext: crypted}, rkEncryptionKey, {
      iv: rkEncryptionIv,
      mode: CryptoJS.mode.CBC,
      padding: CryptoJS.pad.Pkcs7
  });

  return decrypt.toString(CryptoJS.enc.Utf8);
}
