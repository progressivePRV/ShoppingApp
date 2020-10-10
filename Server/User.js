class User{
    constructor(user){
        this._id=user._id;
        this.firstName=user.firstName;
        this.lastName=user.lastName;
        this.gender = user.gender;
        this.email=user.email;
        this.password=user.password;
        this.customerId=user.customerId;
    }

    getUser(){
        var usr = {
            "_id":this._id,
            "firstName":this.firstName,
            "lastName":this.lastName,
            "gender":this.gender,
            "email":this.email,
            "customerId":this.customerId
        }

        return usr;
    }
}
module.exports = User;