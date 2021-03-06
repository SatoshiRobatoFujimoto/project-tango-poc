
#ifndef TANGO_AUGMENTED_REALITY_MESH_H_
#define TANGO_AUGMENTED_REALITY_MESH_H_

#include <tango-gl/bounding_box.h>
#include <tango-gl/drawable_object.h>
#include <tango-gl/segment.h>

#include <Eigen/Core>

#include <mutex>

#include <open_chisel/Chisel.h>
#include <open_chisel/camera/DepthImage.h>
#include <open_chisel/ProjectionIntegrator.h>
#include <open_chisel/geometry/Geometry.h>
#include <open_chisel/camera/PinholeCamera.h>
#include <open_chisel/truncation/Truncator.h>
#include <open_chisel/truncation/QuadraticTruncator.h>
#include <open_chisel/truncation/ConstantTruncator.h>
#include <open_chisel/truncation/QuadraticTruncator.h>
#include <open_chisel/weighting/ConstantWeighter.h>
#include <open_chisel/mesh/Mesh.h>

#include <tango_support_api.h>



typedef boost::shared_ptr<chisel::DepthImage<float>> DepthImagePtr;

namespace tango_augmented_reality {


    class ChiselMesh : public tango_gl::DrawableObject {
    public:
        ChiselMesh();

        ChiselMesh(GLenum render_mode);

        void SetShader();

        void init(TangoCameraIntrinsics intrinsics);

        void Render(const glm::mat4 &projection_mat, const glm::mat4 &view_mat) const;

        void addPoints(glm::mat4 transformation, TangoCameraIntrinsics intrinsics, TangoXYZij *XYZij);

        void updateVertices();
        
        std::mutex render_mutex;

        void clear();

    protected:
        tango_gl::BoundingBox *bounding_box_;

        GLuint uniform_mv_mat_;

        double truncationDistScale;
        double chunkSize;
        double chunkResolution;
        double weighting;
        double carvingDistance;
        bool enableCarving;
        double farClipping;
        double rayTruncation;

        chisel::ChiselPtr chiselMap;
        DepthImagePtr lastDepthImage = DepthImagePtr(new chisel::DepthImage<float>());
        chisel::ProjectionIntegrator projectionIntegrator;

        chisel::Intrinsics chiselIntrinsics;
        chisel::PinholeCamera pinHoleCamera;

    };
}  // namespace tango_augmented_reality
#endif  // TANGO_AUGMENTED_REALITY_MESH_H_
