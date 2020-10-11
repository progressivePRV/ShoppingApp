const express = require("express");
const mongo = require('mongodb');
const jwt = require('jsonwebtoken');
const { requestBody, validationResult, body, header, param, query } = require('express-validator');
const bcrypt = require('bcryptjs');
const User = require("./User");
const braintree = require('braintree');
const { response, request } = require("express");
const fs = require('fs');

const MongoClient = mongo.MongoClient;
const uri = "mongodb+srv://rojatkaraditi:AprApr_2606@test.z8ya6.mongodb.net/project5DB?retryWrites=true&w=majority";
var client;
var collection;
var userItemsCollection;
const tokenSecret = "wFq9+ssDbT#e2H9^";
var decoded={};
var token;

var gateway = new braintree.BraintreeGateway({
    environment: braintree.Environment.Sandbox,
    merchantId: 'bz8nzddwpxkzcb3x',
    publicKey: 'htgfzr9zjz5yx88b',
    privateKey: '77a1d42df2bd79de39a339fff394b3c1'
  });

var connectToUsersDb = function(req,res,next){
    client = new MongoClient(uri, { useNewUrlParser: true, useUnifiedTopology: true});
    client.connect(err => {
      if(err){
          closeConnection();
          return res.status(400).json({"error":"Could not connect to database: "+err});
      }
      collection = client.db("project5DB").collection("users");
      userItemsCollection = client.db("project5DB").collection("userItems");
      console.log("connected to database");
    next();
    });
};

var closeConnection = function(){
    client.close();
}

var verifyToken = function(req,res,next){
    var headerValue = req.header("Authorization");
    if(!headerValue){
        //closeConnection();
        return res.status(400).json({"error":"Authorization header needs to be provided for using API"});
    }

    var authData = headerValue.split(' ');

    if(authData && authData.length==2 && authData[0]==='Bearer'){
        token = authData[1];
        try {
            decoded = jwt.verify(token, tokenSecret);
            next();
          } catch(err) {
            //closeConnection();
            return res.status(400).json({"error":err});
          }
    }
    else {
        //closeConnection();
        return res.status(400).json({"error":"Appropriate authentication information needs to be provided"})
    }

};

const route = express.Router();
route.use("/shop",verifyToken);
route.use("/shop/images",express.static('productImages'));

route.post("/users/signup",connectToUsersDb,[
    body("firstName","firstName cannot be empty").notEmpty().trim().escape(),
    body("firstName","firstName can have only alphabets").isAlpha().trim().escape(),
    body("lastName","lastName cannot be empty").notEmpty().trim().escape(),
    body("lastName","lastName can have only alphabets").isAlpha().trim().escape(),
    body("gender","gender cannot be empty").notEmpty().trim().escape(),
    body("gender","gender can have only alphabets").isAlpha().trim().escape(),
    body("gender","gender can only be Male or Female").isIn(["Male","Female"]),
    body("email","email cannot be empty").notEmpty().trim().escape(),
    body("email","invalid email format").isEmail(),
    body("password","password cannot be empty").notEmpty().trim(),
    body("password","password should have atleast 6 and at max 20 characters").isLength({min:6,max:20})
],(request,response)=>{
    const err = validationResult(request);
    if(!err.isEmpty()){
        closeConnection();
        return response.status(400).json({"error":err});
    }
    try{
        let pwd = request.body.password;
        var hash = bcrypt.hashSync(pwd,10);
        var newUser = new User(request.body);
        newUser.password=hash;

        gateway.customer.create({
            firstName: newUser.firstName,
            lastName: newUser.lastName,
            email: newUser.email
          }, (error, resultResponse) => {

            if(error || !resultResponse.success){
                closeConnection();
                return response.status(400).json({"error":error});
            }
            if(resultResponse.success){
                newUser.customerId=resultResponse.customer.id;
                collection.insertOne(newUser,(err,res)=>{
                    var result={};
                    var responseCode = 200;
                    if(err){
                        // result={"error":err};
                        // responseCode=400;
                        closeConnection();
                        return response.status(400).json({"error":err});
                    }
                    else{
                        if(res.ops.length>0){
                            var usr = res.ops[0].getUser();
                            
                            var userItem={
                                "userId":usr._id,
                                "cartItems":[],
                                "previousOrders":[]
                            }

                            userItemsCollection.insertOne(userItem,(err,reslt)=>{
                                if(err){
                                    result={"error":err};
                                    responseCode=400;
                                }
                                else{
                                    usr.exp = Math.floor(Date.now() / 1000) + (60 * 60);
                                    var token = jwt.sign(usr, tokenSecret);
                                    result=res.ops[0].getUser();
                                    result.token=token;
                                }
                                closeConnection();
                                return response.status(responseCode).json(result);
                            });
                        }
                        else{
                            closeConnection();
                            return response.status(400).json({"error":"user could not be created"});
                        }
                        
                    }
                    //return response.status(responseCode).json(result);
                });
            }
          });
    }
    catch(error){
        closeConnection();
        return response.status(400).json({"error":error});
    }
}); 


route.get("/users/login",connectToUsersDb,[
    header("Authorization","Authorization header required to login").notEmpty().trim()
],(request,response)=>{

    const err = validationResult(request);
    if(!err.isEmpty()){
        closeConnection();
        return response.status(400).json({"error":err});
    }
    
    try{
        var data = request.header('Authorization');
        //console.log(data);
        var authData = data.split(' ');

        if(authData && authData.length==2 && authData[0]==='Basic'){
            let buff = new Buffer(authData[1], 'base64');
            let loginInfo = buff.toString('ascii').split(":");
            var result ={};

            if(loginInfo!=undefined && loginInfo!=null && loginInfo.length==2){
                var query = {"email":loginInfo[0]};
                collection.find(query).toArray((err,res)=>{
                    var responseCode = 400;
                    if(err){
                        result = {"error":err};
                    }
                    else if(res.length<=0){
                        result={"error":"no such user present"};
                    }
                    else{
                        var user = new User(res[0]);
                        if(bcrypt.compareSync(loginInfo[1],user.password)){
                            result=user.getUser();
                            user=user.getUser();
                            user.exp = Math.floor(Date.now() / 1000) + (60 * 60);
                            var token = jwt.sign(user, tokenSecret);
                            result.token=token;
                            responseCode=200;
                        }
                        else{
                            result={"error":"Username or password is incorrect"};
                        }
                    }
                    closeConnection();
                    return response.status(responseCode).json(result);

                });
            }
            else{
                closeConnection();
                return response.status(400).json({"error":"credentials not provided for login"});
            }
        }
        else{
            closeConnection();
            return response.status(400).json({"error":"Desired authentication type and value required for login"})
        }
    }
    catch(error){
        closeConnection();
        return response.status(400).json({"error":error.toString()});
    }

});

route.get("/shop/items",(request,response)=>{
    try{
        var rawdata = fs.readFileSync('discount.json');
        if(!rawdata){
            //closeConnection();
            return response.status(400).json({"error":"No items file found"});
        }
        var items = JSON.parse(rawdata);
        //closeConnection();
        return response.status(200).json(items);
    }
    catch(error){
        //closeConnection();
        return response.status(400).json({"error":error.toString()});
    }
});

route.get("/shop/items/:id",[
    param('id','id should be an integer value').isInt()
],(request,response)=>{
    var err = validationResult(request);
    if(!err.isEmpty()){
        //closeConnection();
        return response.status(400).json({"error":err});
    }

    try{
        var rawdata = fs.readFileSync('discount.json');
        if(!rawdata){
            //closeConnection();
            return response.status(400).json({"error":"No items file found"});
        }
        var items = JSON.parse(rawdata);
        if(items && items.results && items.results.length>0){
            var item = items.results.find(it=>it.id==request.params.id);
            if(item){
                //closeConnection();
                return response.status(200).json(item);
            }
            else{
                //closeConnection();
                return response.status(400).json({"error":"No item found with id "+request.params.id});
            }
        }
        else{
            //closeConnection();
            return response.status(400).json({"error":"No items present"});
        }
        
    }
    catch(error){
        //closeConnection();
        return response.status(400).json({"error":error.toString()});
    }
});

route.put('/shop/items',connectToUsersDb,[
    body('id','id is required for adding item to cart').notEmpty().trim().escape(),
    body('id','id should be an integer value').isInt(),
    body('quantity','quantity is required for adding item to cart').notEmpty().trim().escape(),
    body('quantity','quantity should be a valid integer value gretaer than 0').isInt({gt:0}),
    body('operation','operation is required for adding item to cart').notEmpty().trim().escape(),
    body('operation','operation can only be add or remove').isIn(['add','remove'])
],(request,response)=>{
    var err = validationResult(request);
    if(!err.isEmpty()){
        closeConnection();
        return response.status(400).json({"error":err});
    }

    var query = {"userId":new mongo.ObjectID(decoded._id)};
    userItemsCollection.find(query,{ projection: { cartItems: 1 } }).toArray((err,res)=>{
        if(err){
            closeConnection();
            return response.status(400).json({"error":err});
        }
        else if(res.length<=0){
            closeConnection();
            return response.status(400).json({"error":"no cart found for user"});
        }
        else{
            var items = res[0].cartItems;
            //console.log(res);
            var item = items.find(it => it.id==request.body.id);
            if(item){
                var idx = items.indexOf(item);
                if(request.body.operation==='add'){
                    items.splice(idx,1);
                    item.quantity=(parseInt(item.quantity) + parseInt(request.body.quantity));
                    items.push(item);
                }
                else if(request.body.operation==='remove'){
                    if(parseInt(request.body.quantity)>parseInt(item.quantity)){
                        closeConnection();
                        return response.status(400).json({"error":"trying to remove more items than present in cart"});
                    }
                    else if(parseInt(request.body.quantity)==parseInt(item.quantity)){
                        items.splice(idx,1);
                    }
                    else{
                        items.splice(idx,1);
                        item.quantity=parseInt(item.quantity) - parseInt(request.body.quantity);
                        items.push(item);
                    }
                }
            }
            else{
                if(request.body.operation==='add'){
                    var rawdata = fs.readFileSync('discount.json');
                    if(!rawdata){
                        closeConnection();
                        return response.status(400).json({"error":"No items file found"});
                    }
                    var data = JSON.parse(rawdata);
                    var newItem = data.results.find(it=>it.id==request.body.id);
                    if(newItem){
                        var addItem = {
                            "id":request.body.id,
                            "quantity":parseInt(request.body.quantity)
                        }
                        items.push(addItem);
                    }
                    else{
                        closeConnection();
                        return response.status(400).json({"error":"No item with id "+request.body.id+" exists"});
                    }
                }
                else if(request.body.operation==='remove'){
                    closeConnection();
                    return response.status(400).json({"error":"Item not present in cart to remove"});
                }
            }

            var updatedData = {
                "cartItems":items
            };
            var newQuery = {$set : updatedData};
            userItemsCollection.updateOne(query,newQuery,(err,reslt)=>{
                if(err){
                    closeConnection();
                    return response.status(400).json({"error":"cart could not be updated"});
                }
                else{
                    closeConnection();
                    return response.status(200).json({"result":"cart updated"});
                }
            })
        }
    });

});

route.get('/shop/cart',connectToUsersDb,(request,response)=>{
    var query = {"userId":new mongo.ObjectID(decoded._id)};
    userItemsCollection.find(query).toArray((err,res)=>{
        if(err){
            closeConnection();
            return response.status(400).json({"error":err});
        }
        else if(res.length<=0){
            closeConnection();
            return response.status(400).json({"error":"no cart found for user"});
        }
        else{
            var rawdata = fs.readFileSync('discount.json');
            if(!rawdata){
                closeConnection();
                return response.status(400).json({"error":"No items file found"});
            }
            var data = JSON.parse(rawdata);
            
            var items = res[0].cartItems;
            if(items.length<=0){
                closeConnection();
                return response.status(200).json({"total":0.00});
            }
            else{
                var cnt = items.length;
                var discountPrice=0.0;
                var cart=[];
                items.forEach(item=>{
                    var newItem = data.results.find(it=>it.id==item.id);
                    var totalPrice = parseFloat(newItem.price)*parseFloat(item.quantity);
                    var discount = (totalPrice*newItem.discount)/100;
                    discountPrice = parseFloat(discountPrice) + (parseFloat(totalPrice)-parseFloat(discount));
                    newItem.quantity=item.quantity;
                    cart.push(newItem);
                    cnt--;
                    if(cnt==0){
                        var result = {
                            "total":discountPrice.toFixed(2),
                            "cart":cart
                        }
                        closeConnection();
                        return response.status(200).json(result);
                    }
                });
            }
        }
    });
});

route.get("/shop/customerToken",connectToUsersDb,(request,response)=>{
    var query = {"_id":new mongo.ObjectID(decoded._id)};
    collection.find(query).toArray((err,res)=>{
        if(err){
            closeConnection();
            return response.status(400).json({"error":err});
        }
        else if(res.length<=0){
            closeConnection();
            return response.status(400).json({"error":"no user found with id "+decoded._id});
        }
        else{
            var user = new User(res[0]).getUser();
            gateway.clientToken.generate({
                customerId: user.customerId,
                options:{
                    //failOnDuplicatePaymentMethod: true,
                    verifyCard: true
                }
              }, (err, res) => {
                  if(err){
                      closeConnection();
                      return response.status(400).json({"error":err});
                  }
                  if(res && res.clientToken){
                    const clientToken = res.clientToken;
                    closeConnection();
                    return response.status(200).json({"clientToken":clientToken})
                  }
                  else{
                    closeConnection();
                    return response.status(400).json({"error":"client token could not be generated"}); 
                  }
              });
        }
    });
});

route.post("/shop/checkout",connectToUsersDb,[
    body("amount","amount is required to make payment").notEmpty().trim().escape(),
    body("amount","amount should be a numeric value").isNumeric(),
    body("nonce","nonce is required to make payment").notEmpty().trim().escape(),
    body("deviceData","deviceData is required to make payment").notEmpty().trim().escape(),
    body("date","date needs to be provided to checkout").notEmpty().trim().escape(),
    body("address","address required to checkout").notEmpty().trim().escape(),
    body("city","city required to checkout").notEmpty().trim().escape(),
    body("state","state required to checkout").notEmpty().trim().escape(),
    body("zipCode","zip code required to checkout").notEmpty().trim().escape(),
    body("zipCode","zip code is invalid").isInt().isLength({min:5,max:5}),
    body("phoneNumber","phone number needed to checkout").notEmpty().trim().escape(),
    body("phoneNumber","phone should be valid").isMobilePhone()
],(request,response)=>{
    var checkErr = validationResult(request);
    if(!checkErr.isEmpty()){
        closeConnection();
        return response.status(400).json({"error":checkErr});
    }

    var query = {"userId":new mongo.ObjectID(decoded._id)};
    userItemsCollection.find(query).toArray((err,res)=>{
        if(err){
            closeConnection();
            return response.status(400).json({"error":err});
        }
        if(res.length<=0){
            closeConnection();
            return response.status(400).json({"error":"no user found with id "+decoded._id});
        }
        var userCart = res[0];
        if(!userCart.cartItems || !userCart.previousOrders || userCart.cartItems.length<=0){
            closeConnection();
            return response.status(400).json({"error":"cannot proceed, user cart is improper"});
        }
        gateway.transaction.sale({
            amount: request.body.amount,
            paymentMethodNonce: request.body.nonce,
            deviceData: request.body.deviceData,
            options: {
              submitForSettlement: true
            }
          }, (error, result) => {
              if(error){
                  closeConnection();
                  return response.status(400).json({"error":error});
              }
            if (!result.success) {
                closeConnection();
                return response.status(400).json({"error":"payment could not be processed"});
            } else {
                    
                    var newPreviousOrder = {
                        "amount":request.body.amount,
                        "date":request.body.date,
                        "address":request.body.address,
                        "city":request.body.city,
                        "state":request.body.state,
                        "zipCode":request.body.zipCode,
                        "phoneNumber":request.body.phoneNumber,
                        "items":userCart.cartItems
                    };
                    delete userCart._id;
                    delete userCart.userId;
                    userCart.previousOrders.push(newPreviousOrder);
                    userCart.cartItems=[];
    
                    var newQuery={$set:userCart};
                    userItemsCollection.updateOne(query,newQuery,(e,reslt)=>{
                        if(e){
                            closeConnection();
                            return response.status(400).json({"error":e});
                        }
                        else{
                            closeConnection();
                            return response.status(200).json({"result":"payment successful"});
                        }
                    });
                // closeConnection();
                // return response.status(200).json({"result":result.transaction});
            }
          });
    });
});

route.get("/shop/orders",connectToUsersDb,(request,response)=>{
    var query = {"userId":new mongo.ObjectID(decoded._id)};
    userItemsCollection.find(query).toArray((err,res)=>{
        if(err){
            closeConnection();
            return response.status(400).json({"error":err});
        }
        if(res.length<=0){
            closeConnection();
            return response.status(400).json({"error":"no user found with id "+decoded._id});
        }
        var userCart = res[0];
        if(!userCart.previousOrders || userCart.previousOrders.length<=0){
            closeConnection();
            return response.status(400).json({"error":"no previous orders found"});
        }

        var rawdata = fs.readFileSync('discount.json');
        if(!rawdata){
            closeConnection();
            return response.status(400).json({"error":"No items file found"});
        }
        var data = JSON.parse(rawdata);

        var orderItems=[];
        for(var i=0;i<userCart.previousOrders.length;i++){
            var item = userCart.previousOrders[i];
            var items=[];
            for(var j=0;j<item.items.length;j++){
                var newItem = data.results.find(it=>it.id==item.items[j].id);
                newItem.quantity=item.items[j].quantity;
                items.push(newItem);
            }
            item.items=items;
            orderItems.push(item);
        }
        closeConnection();
        return response.status(200).json(orderItems);
    });
});

route.delete("/shop/cart",connectToUsersDb,(request,response)=>{
    var query = {"userId":new mongo.ObjectID(decoded._id)};
    userItemsCollection.find(query).toArray((err,res)=>{
        if(err){
            closeConnection();
            return response.status(400).json({"error":err});
        }
        if(res.length<=0){
            closeConnection();
            return response.status(400).json({"error":"no user found with id "+decoded._id});
        }
        var userCart = res[0];
        if(!userCart.cartItems || userCart.cartItems.length<=0){
            closeConnection();
            return response.status(400).json({"error":"no items found in cart"});
        }
        var newQuery={$set:{"cartItems":[]}};
        userItemsCollection.updateOne(query,newQuery,(e,reslt)=>{
            if(e){
                closeConnection();
                return response.status(400).json({"error":err});
            }
            else{
                closeConnection();
                return response.status(200).json({"result":"cart emptied"});
            }
        })
    })
});

route.get("/shop/user/profile",connectToUsersDb,(request,response)=>{
    var query = {"_id":new mongo.ObjectID(decoded._id)};
    collection.find(query).toArray((err,res)=>{
        if(err){
            closeConnection();
            return response.status(400).json({"error":err});
        }
        if(res.length<=0){
            closeConnection();
            return response.status(400).json({"error":"no user found with id "+decoded._id});
        }
        var user = new User(res[0]).getUser();
        closeConnection();
        return response.status(200).json(user);
    });
});

module.exports = route; 