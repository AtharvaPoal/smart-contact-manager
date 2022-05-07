console.log("this is script file")


const toggleSidebar=()=>{
	
	if($('.sidebar').is(":visible")){
		$(".sidebar").css("display","none");
		$(".content").css("margin-left","0%");
	}else{
		$(".sidebar").css("display","block");
		$(".content").css("margin-left","20%");
		
	}
	
};


const search=()=>{
//	console.log("searching...");

let query=$("#search-input").val();


if(query==''){
	$(".search-result").hide();

}else{
	//search
	console.log(query);
	
	//sending request to server
	let url=`http://localhost:8282/search/${query}`;
	
	fetch(url).then((response)=>{
		return response.json();
	}).then((data)=>{
		console.log(data);
		
		let text=`<div class="list-group">`
	
		
		data.forEach(contact=>{
			text+=`<a href="/user/${contact.cId}/contact" class='list-group-item list-group-item-action'>${contact.name}</a>`
		});
		
			
		text+=`</div>`;
		
		$(".search-result").html(text);
		$(".search-result").show();
	});
	
}
};




//first request to server to create order


const paymentStart=()=>{
    console.log("payment started");
	let amount=$("#payment_field").val();
	console.log(amount);


	if(amount=='' || amount==null){
		swal("Failed", "amount is required!!", "error");
	    return;
	}

  //we will use ajax to send request to server to create order

  $.ajax({
		 url:"/user/create_order",
		 data:JSON.stringify({amount:amount,info:"order_request"}),
		 contentType:"application/json",
		 type:"POST",
		 dataType:"json",
		 success:function(response){
			 console.log(response);
			 if(response.status=="created"){
				 //open payment form

				 let options={
					key:"rzp_test_zDFgjeFwoe7aQ5",
					amount:response.amount,
                    currency:"INR",
					name:"Smart Contact Manager",
					description:"Donation",
					image:"https://www.pexels.com/photo/clear-glass-sphere-302743/",   
				    order_id:response.id,
					handler:function(response){
						console.log(response.razorpay_payment_id);
						console.log(response.razorpay_order_id);
						console.log(response.razorpay_signature);
						
						updatePaymentOnServer(
							response.razorpay_payment_id,
							response.razorpay_order_id,
							"paid"
						);
						
					},
					"prefill": {
						"name": "",
						"email": "",
						"contact": ""
						},
						"notes": {
						"address": "cricket"
						
						},
						"theme": {
						"color": "#3399cc"
						}
				};


				let rzp = new Razorpay(options);
				rzp.open();

				rzp.on('payment.failed', function (response){
					console.log(response.error.code);
					console.log(response.error.description);
					console.log(response.error.source);
					console.log(response.error.step);
					console.log(response.error.reason);
					console.log(response.error.metadata.order_id);
					console.log(response.error.metadata.payment_id);
					swal("Failed", "Oops!! Payment failed", "error");
				});


				}
		 },
		 error:function(error){
			 console.log(error);
			 alert("something went wrong!!");
		 },
	});

};

updatePaymentOnServer(payment_id,order_id,status)
{$.ajax({
	     url:"/user/update_order",
		 data:JSON.stringify({
			payment_id: payment_id,
			order_id: order_id,
			status: status,
		}),
		 contentType:"application/json",
		 type:"POST",
		 dataType:"json",
		 success:function(response){		
			swal("Success", "Payment Successful", "success");
			},
		 error:function(response){
			swal("Failed", "Payment Successful, but did not get on server", "error");
		},
});	
}
	