class Swish(Module):
  __parameters__ = []
  __buffers__ = []
  training : bool
  def forward(self: __torch__.wenet.transformer.swish.Swish,
    x: Tensor) -> Tensor:
    return torch.mul(x, torch.sigmoid(x))
