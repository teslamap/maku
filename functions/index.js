const {onCall, HttpsError}=require("firebase-functions/v2/https");
const admin=require("firebase-admin");
admin.initializeApp();
const ADMIN_UID="D2N37vcj9tgGKdhw0oXKOhetK3q1";
exports.createManager=onCall({region:"us-central1"},async(request)=>{
  if(!request.auth || request.auth.uid!==ADMIN_UID) throw new HttpsError("permission-denied","Only the primary admin can create managers.");
  const {name,email,password}=request.data||{};
  if(typeof name!=="string"||!name.trim()||typeof email!=="string"||!/^\S+@\S+\.\S+$/.test(email)||typeof password!=="string"||password.length<8||password.length>128) throw new HttpsError("invalid-argument","Valid name, email and password (8-128 characters) are required.");
  let user;
  try { user=await admin.auth().createUser({displayName:name.trim(),email:email.trim().toLowerCase(),password, emailVerified:false, disabled:false}); await admin.auth().setCustomUserClaims(user.uid,{role:"manager"}); await admin.firestore().collection("users").doc(user.uid).set({name:name.trim(),email:email.trim().toLowerCase(),role:"manager",createdAt:new Date().toISOString()}); return {uid:user.uid,email:user.email}; }
  catch(e){ if(user) await admin.auth().deleteUser(user.uid).catch(()=>{}); if(e instanceof HttpsError) throw e; throw new HttpsError("internal","Manager account could not be created."); }
});