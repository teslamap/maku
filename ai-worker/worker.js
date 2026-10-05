const MODEL="@cf/zai-org/glm-4.7-flash";
const cors={"Access-Control-Allow-Origin":"*","Access-Control-Allow-Methods":"POST, OPTIONS","Access-Control-Allow-Headers":"Content-Type"};
function json(data,status=200){return new Response(JSON.stringify(data),{status,headers:{"Content-Type":"application/json; charset=utf-8",...cors}})}
function cleanProducts(items){return(Array.isArray(items)?items:[]).slice(0,120).map(p=>({id:String(p.id||""),name:String(p.name||""),category:String(p.category||""),price:Number(p.price)||0,oldPrice:Number(p.oldPrice)||0,stock:Number(p.stock)||0,discount:Number(p.discount)||0,description:String(p.description||"").slice(0,500),status:String(p.status||"active")})).filter(p=>p.id&&p.status!=="inactive")}
function analyzeQuery(message,products){
  const q=message.toLowerCase();
  const nums=q.match(/\d+(?:[.,]\d+)?/g)||[];
  const budget=nums.length?Math.max(...nums.map(x=>Number(x.replace(",",".")))):null;
  const wantsBudget=budget!==null&&/(ლარ|₾|მდე|ბიუჯეტ|ფას)/.test(q);
  const wantsSale=/(აქცი|ფასდაკლებ|sale|discount|დაკლებული)/.test(q);
  const wantsCheap=/(ყველაზე იაფ|იაფი|ბიუჯეტური|იაფად)/.test(q);
  const wantsExpensive=/(ყველაზე ძვირ|პრემიუმ|ძვირი)/.test(q);
  let candidates=products.filter(p=>p.stock>0);
  if(wantsBudget)candidates=candidates.filter(p=>p.price<=budget);
  if(wantsSale)candidates=candidates.filter(p=>p.oldPrice>p.price||p.discount>0);
  const terms=q.replace(/[^\p{L}\p{N}\s]/gu," ").split(/\s+/).filter(w=>w.length>=3).filter(w=>!["მინდა","მაქვს","რომელ","არის","რამე","რა","გაქვთ","მომიძებნე","მირჩიე","შეგიძლია","პროდუქტი"].includes(w));
  let scored=candidates.map(p=>{
    const text=(p.name+" "+p.category+" "+p.description).toLowerCase();
    let score=0;
    terms.forEach(t=>{if(text.includes(t))score+=4;});
    if(wantsSale&&(p.oldPrice>p.price||p.discount>0))score+=3;
    return {...p,_score:score};
  });
  if(terms.length)scored=scored.filter(p=>p._score>0);
  if(wantsCheap)scored.sort((a,b)=>a.price-b.price);
  else if(wantsExpensive)scored.sort((a,b)=>b.price-a.price);
  else scored.sort((a,b)=>b._score-a._score);
  return {budget,wantsBudget,wantsSale,wantsCheap,wantsExpensive,candidates:scored.slice(0,12).map(p=>p.id)};
}
function systemPrompt(products,analysis){
  return "შენ ხარ Makasia ონლაინ მაღაზიის ჭკვიანი AI კონსულტანტი. პასუხობ ქართულად, ბუნებრივად და მოკლედ.\n"+
  "მკაცრი წესები: არასოდეს მოიგონო პროდუქტი, ფასი, მარაგი, ფასდაკლება ან მახასიათებელი. გამოიყენე მხოლოდ კატალოგი. მარაგი 0 ნიშნავს ამოწურულს. ბიუჯეტის შემთხვევაში არ გადააცილო ბიუჯეტს. აქციის შემთხვევაში გამოიყენე მხოლოდ oldPrice>price ან discount>0. თუ მომხმარებელი ამბობს მირჩიე, თავად შეარჩიე 1-3 საუკეთესო. თუ შედარება სურს, შეადარე მხოლოდ რეალური მონაცემებით. თუ ინფორმაცია არ არის, პირდაპირ თქვი.\n"+
  "დააბრუნე მხოლოდ JSON: {\"answer\":\"პასუხი ქართულად\",\"productIds\":[\"id1\"]}. productIds მაქსიმუმ 5 და მხოლოდ კატალოგიდან.\n"+
  "ავტომატური ანალიზი:\n"+JSON.stringify(analysis)+"\nკატალოგი:\n"+JSON.stringify(products);
}
export default {async fetch(request,env){if(request.method==="OPTIONS")return new Response(null,{headers:cors});if(request.method!=="POST")return json({error:"POST required"},405);try{const body=await request.json();const message=String(body.message||"").trim();if(!message||message.length>1200)return json({error:"Invalid message"},400);const products=cleanProducts(body.products);const analysis=analyzeQuery(message,products);const focusedIds=new Set(analysis.candidates);const focused=analysis.candidates.length?products.filter(p=>focusedIds.has(p.id)):products.slice(0,80);const history=Array.isArray(body.history)?body.history.slice(-10).map(x=>({role:x?.role==="assistant"?"assistant":"user",content:String(x?.content||"").slice(0,1000)})):[];const messages=[{role:"system",content:systemPrompt(focused,analysis)},...history,{role:"user",content:message}];const result=await env.AI.run(MODEL,{messages,max_tokens:450,temperature:0.25});let raw=typeof result?.response==="string"?result.response:"";raw=raw.replace(/\`\`\`json/gi,"").replace(/\`\`\`/g,"").trim();let parsed;try{parsed=JSON.parse(raw)}catch{const m=raw.match(/\{[\s\S]*\}/);if(m)parsed=JSON.parse(m[0])}if(!parsed||typeof parsed.answer!=="string")return json({answer:raw||"ამ კითხვაზე პასუხის გაცემა ვერ შევძელი.",productIds:[]});const allowed=new Set(products.map(p=>p.id));const productIds=Array.isArray(parsed.productIds)?parsed.productIds.map(String).filter(id=>allowed.has(id)).slice(0,5):[];return json({answer:parsed.answer.slice(0,3000),productIds})}catch(error){console.error("Makasia AI error",error);return json({error:"AI temporarily unavailable"},502)}}};