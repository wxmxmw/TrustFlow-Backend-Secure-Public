async function main() {
  const ProofRegistry = await ethers.getContractFactory("ProofRegistry");
  const contract = await ProofRegistry.deploy();

  await contract.deployed();

  console.log("ProofRegistry deployed to:", contract.address);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
